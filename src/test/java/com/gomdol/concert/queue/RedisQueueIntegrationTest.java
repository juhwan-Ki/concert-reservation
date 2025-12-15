package com.gomdol.concert.queue;

import com.gomdol.concert.common.TestContainerConfig;
import com.gomdol.concert.common.TestDataFactory;
import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import com.gomdol.concert.queue.application.port.in.EnterQueuePort;
import com.gomdol.concert.queue.application.port.in.IssueQueueTokenPort;
import com.gomdol.concert.queue.application.port.in.PromoteTokenPort;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;
import com.gomdol.concert.show.infra.persistence.entity.ShowEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.gomdol.concert.queue.application.port.in.IssueQueueTokenPort.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis 대기열 시스템 통합 테스트")
@Import(TestContainerConfig.class)
class RedisQueueIntegrationTest {

    @Autowired
    private IssueQueueTokenPort issueQueueTokenPort;

    @Autowired
    private EnterQueuePort enterQueuePort;

    @Autowired
    private PromoteTokenPort promoteTokenPort;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TestDataFactory testDataFactory;

    private Long testShowId;

    @BeforeEach
    void setUp() {
        // Redis 데이터 정리
        cleanupRedis();

        // 테스트 데이터 생성
        VenueEntity venue = testDataFactory.createVenue("Test Venue", "Seoul", 100);
        ConcertEntity concert = testDataFactory.createConcert("Test Concert", venue);
        ShowEntity show = testDataFactory.createShow(concert, LocalDateTime.now().plusDays(7), 100);
        testShowId = show.getId();
    }

    @AfterEach
    void tearDown() {
        cleanupRedis();
    }

    private void cleanupRedis() {
        // queue 관련 모든 키 삭제
        Set<String> keys = redisTemplate.keys("queue:*");
        if (!keys.isEmpty())
            redisTemplate.delete(keys);
    }

    @Test
    @DisplayName("토큰 발급 - 첫 50명은 ENTERED, 이후는 WAITING")
    void issue_token_first_50_entered_then_waiting() {
        // given
        Long targetId = testShowId;

        // when: 60명의 사용자가 토큰 발급
        List<QueueTokenResponse> responses = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            QueueTokenResponse response = issueQueueTokenPort.issue(new IssueCommand("user" + i, targetId, "key" + i));
            responses.add(response);
        }

        // then: 처음 50명은 ENTERED
        for (int i = 0; i < 50; i++) {
            assertThat(responses.get(i).status()).isEqualTo(QueueStatus.ENTERED.name());
            assertThat(responses.get(i).position()).isEqualTo(0);
        }

        // then: 51번째부터는 WAITING
        for (int i = 50; i < 60; i++) {
            assertThat(responses.get(i).status()).isEqualTo(QueueStatus.WAITING.name());
            assertThat(responses.get(i).position()).isGreaterThan(0);
        }

        // then: WAITING 순번이 순차적으로 증가
        for (int i = 50; i < 59; i++)
            assertThat(responses.get(i).position()).isLessThan(responses.get(i + 1).position());
    }

    @Test
    @DisplayName("동일 사용자가 여러 번 발급해도 같은 토큰")
    void idempotency_same_user_same_token() {
        // given
        Long targetId = testShowId;
        String userId = "user123";
        IssueCommand command = new IssueCommand(userId, targetId, "key1");

        // when: 3번 발급
        QueueTokenResponse first = issueQueueTokenPort.issue(command);
        QueueTokenResponse second = issueQueueTokenPort.issue(command);
        QueueTokenResponse third = issueQueueTokenPort.issue(command);

        // then: 모두 같은 토큰
        assertThat(second.token()).isEqualTo(first.token());
        assertThat(third.token()).isEqualTo(first.token());
        assertThat(second.status()).isEqualTo(first.status());
        assertThat(third.status()).isEqualTo(first.status());
    }

    @Test
    @DisplayName("동시 요청에도 하나의 토큰만 발급")
    void idempotency_concurrent_requests_one_token() throws InterruptedException {
        // given
        Long targetId = testShowId;
        String userId = "concurrent-user";
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<QueueTokenResponse> responses = new ArrayList<>();

        // when: 10개 스레드에서 동시에 발급 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    QueueTokenResponse response = issueQueueTokenPort.issue(new IssueCommand(userId, targetId, "key" + index));
                    synchronized (responses) {
                        responses.add(response);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 모든 응답이 같은 토큰
        String firstToken = responses.get(0).token();
        assertThat(responses).allMatch(r -> r.token().equals(firstToken));

        // then: Redis에 토큰이 하나만 존재
        String userTokenKey = String.format("queue:{%d}:user:%s", targetId, userId);
        String storedToken = redisTemplate.opsForValue().get(userTokenKey);
        assertThat(storedToken).isEqualTo(firstToken);
    }

    @Test
    @DisplayName("입장 인원 확인 시 만료된 토큰 자동 제거")
    void removes_expired_tokens_on_count() {
        // given
        Long targetId = testShowId;

        // 50명 입장 (ENTERED)
        for (int i = 1; i <= 50; i++)
            issueQueueTokenPort.issue(new IssueCommand("user" + i, targetId, "key" + i));

        // Redis SortedSet 크기 확인 (50개)
        String enteredKey = String.format("queue:{%d}:entered", targetId);
        Long beforeSize = redisTemplate.opsForZSet().size(enteredKey);
        assertThat(beforeSize).isEqualTo(50);

        // when: 만료 시간을 과거로 설정 (강제 만료)
        // score를 현재 시간보다 과거로 변경
        Set<String> members = redisTemplate.opsForZSet().range(enteredKey, 0, -1);
        long pastTime = System.currentTimeMillis() - 100000; // 과거 시간
        for (String member : members)
            redisTemplate.opsForZSet().add(enteredKey, member, pastTime);

        // 다음 사용자 입장 시도
        QueueTokenResponse newUserResponse = issueQueueTokenPort.issue(new IssueCommand("new-user", targetId, "new-key"));

        // then: 만료된 토큰이 자동으로 제거됨 (countEnteredActiveWithLock 호출 시)
        Long afterSize = redisTemplate.opsForZSet().size(enteredKey);
        assertThat(afterSize).isLessThanOrEqualTo(1); // 만료된 토큰 제거됨, 새 사용자만 남음

        // then: 새 사용자는 ENTERED 상태
        assertThat(newUserResponse.status()).isEqualTo(QueueStatus.ENTERED.name());
    }

    @Test
    @DisplayName("승급 처리 - WAITING → ENTERED")
    void promote_waiting_to_entered() {
        // given
        Long targetId = testShowId;

        // 60명 발급 (50명 ENTERED, 10명 WAITING)
        for (int i = 1; i <= 60; i++)
            issueQueueTokenPort.issue(new IssueCommand("user" + i, targetId, "key" + i));

        // WAITING 사용자 확인
        String waitingKey = String.format("queue:{%d}:waiting", targetId);
        Long waitingCount = redisTemplate.opsForZSet().size(waitingKey);
        assertThat(waitingCount).isEqualTo(10);

        // when: ENTERED 토큰을 과거로 설정 (만료 시킴) → 승급 가능하도록
        String enteredKey = String.format("queue:{%d}:entered", targetId);
        Set<String> members = redisTemplate.opsForZSet().range(enteredKey, 0, -1);
        long pastTime = System.currentTimeMillis() - 100000;
        for (String member : members)
            redisTemplate.opsForZSet().add(enteredKey, member, pastTime);

        // 승급 실행
        int promoted = promoteTokenPort.promote(targetId);

        // then
        assertThat(promoted).isGreaterThan(0);

        // WAITING 감소, ENTERED 증가
        Long afterWaitingCount = redisTemplate.opsForZSet().size(waitingKey);
        Long afterEnteredCount = redisTemplate.opsForZSet().size(enteredKey);
        assertThat(afterWaitingCount).isLessThan(waitingCount);
        assertThat(afterEnteredCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("순번 계산 - ZRANK로 실시간 계산")
    void position_calculated_by_zrank() {
        // given
        Long targetId = testShowId;

        // 50명 채우고 10명 대기
        for (int i = 1; i <= 60; i++)
            issueQueueTokenPort.issue(new IssueCommand("user" + i, targetId, "key" + i));

        // when: user51, user60의 토큰 조회 후 순번 확인
        QueueTokenResponse issued51 = issueQueueTokenPort.issue(new IssueCommand("user51", targetId, "key51"));
        QueueTokenResponse issued60 = issueQueueTokenPort.issue(new IssueCommand("user60", targetId, "key60"));

        EnterQueuePort.QueueTokenRequest request51 = new EnterQueuePort.QueueTokenRequest(targetId, "user51", issued51.token());
        QueueTokenResponse response51 = enterQueuePort.enterQueue(request51);

        EnterQueuePort.QueueTokenRequest request60 = new EnterQueuePort.QueueTokenRequest(targetId, "user60", issued60.token());
        QueueTokenResponse response60 = enterQueuePort.enterQueue(request60);

        // then: user51이 user60보다 앞순번
        assertThat(response51.position()).isLessThan(response60.position());
        assertThat(response51.position()).isEqualTo(1); // 첫 번째 대기자
        assertThat(response60.position()).isEqualTo(10); // 마지막 대기자
    }

    @Test
    @DisplayName("TTL 자동 만료 - Hash/String은 TTL로 자동 삭제")
    void ttl_auto_expiration() {
        // given
        Long targetId = testShowId;
        String userId = "ttl-test-user";

        // when
        QueueTokenResponse response = issueQueueTokenPort.issue(new IssueCommand(userId, targetId, "ttl-key"));

        // then있음
        String tokenKey = String.format("queue:{%d}:token:%s", targetId, response.token());
        String userTokenKey = String.format("queue:{%d}:user:%s", targetId, userId);

        Long tokenTTL = redisTemplate.getExpire(tokenKey, TimeUnit.SECONDS);
        Long userTokenTTL = redisTemplate.getExpire(userTokenKey, TimeUnit.SECONDS);

        // TTL이 설정되어 있고, 0보다 큼
        assertThat(tokenTTL).isGreaterThan(0);
        assertThat(userTokenTTL).isGreaterThan(0);

        // ENTERED는 180초 + 60초 = 240초 근처
        // WAITING은 1800초 + 60초 = 1860초 근처
        if (response.status().equals(QueueStatus.ENTERED.name()))
            assertThat(tokenTTL).isLessThanOrEqualTo(240);
        else
            assertThat(tokenTTL).isLessThanOrEqualTo(1860);
    }

    @Test
    @DisplayName("capacity 초과 시 대기열 진입")
    void waiting_when_capacity_exceeded() {
        // given
        Long targetId = testShowId;

        // when: capacity(50) + 1 발급
        List<QueueTokenResponse> responses = new ArrayList<>();
        for (int i = 1; i <= 51; i++) {
            QueueTokenResponse response = issueQueueTokenPort.issue(new IssueCommand("user" + i, targetId, "key" + i));
            responses.add(response);
        }

        // then: 51번째는 WAITING
        assertThat(responses.get(50).status()).isEqualTo(QueueStatus.WAITING.name());
        assertThat(responses.get(50).position()).isEqualTo(1);
    }

    @Test
    @DisplayName("대기 중인 사용자가 있으면 신규 사용자는 무조건 WAITING")
    void new_user_waiting_when_existing_waiting_users() {
        // given
        Long targetId = testShowId;

        // 51명 발급 (50명 ENTERED, 1명 WAITING)
        for (int i = 1; i <= 51; i++)
            issueQueueTokenPort.issue(new IssueCommand("user" + i, targetId, "key" + i));

        // ENTERED 중 1명 만료시켜서 capacity 확보
        String enteredKey = String.format("queue:{%d}:entered", targetId);
        Set<String> members = redisTemplate.opsForZSet().range(enteredKey, 0, 0);
        if (members != null && !members.isEmpty()) {
            String member = members.iterator().next();
            long pastTime = System.currentTimeMillis() - 100000;
            redisTemplate.opsForZSet().add(enteredKey, member, pastTime);
        }

        // when
        QueueTokenResponse newUser = issueQueueTokenPort.issue(new IssueCommand("new-user", targetId, "new-key"));

        // then
        assertThat(newUser.status()).isEqualTo(QueueStatus.WAITING.name());
    }

    @Test
    @DisplayName("동시 발급 테스트 - 각자 다른 토큰 발급")
    void concurrent_issue_different_tokens() throws InterruptedException {
        // given
        Long targetId = testShowId;

        // capacity 채우기
        for (int i = 1; i <= 50; i++)
            issueQueueTokenPort.issue(new IssueCommand("filler" + i, targetId, "filler-key" + i));

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<QueueTokenResponse> responses = new ArrayList<>();

        // when: 20명이 동시에 발급
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    QueueTokenResponse response = issueQueueTokenPort.issue(new IssueCommand("concurrent-user" + index, targetId, "concurrent-key" + index));
                    synchronized (responses) {
                        responses.add(response);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 모두 WAITING
        assertThat(responses).allMatch(r -> r.status().equals(QueueStatus.WAITING.name()));
        assertThat(responses).hasSize(threadCount);

        // 모든 토큰이 다름 (중복 발급 없음)
        List<String> tokens = responses.stream().map(QueueTokenResponse::token).toList();
        long distinctTokenCount = tokens.stream().distinct().count();
        assertThat(distinctTokenCount).isEqualTo(threadCount);

        // 순번은 1~20 범위 내 (동시 발급으로 순번이 겹칠 수 있지만, 범위는 유효해야 함)
        assertThat(responses).allMatch(r -> r.position() >= 1 && r.position() <= threadCount);
    }
}
