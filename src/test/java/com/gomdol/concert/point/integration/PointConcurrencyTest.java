package com.gomdol.concert.point.integration;

import com.gomdol.concert.common.TestContainerConfig;
import com.gomdol.concert.point.application.port.in.GetPointBalancePort;
import com.gomdol.concert.point.application.port.in.SavePointPort;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.infra.persistence.PointJpaRepository;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("포인트 동시성 테스트")
@Import(TestContainerConfig.class)
class PointConcurrencyTest {

    @Autowired
    private SavePointPort savePointPort;

    @Autowired
    private GetPointBalancePort getPointBalancePort;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        pointJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("동일 사용자가 동시에 포인트 충전 - 모두 성공해야 함")
    void concurrentCharge_allSucceed() throws InterruptedException {
        // given
        int threadCount = 3;
        long chargeAmount = 1000L;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 3번 동시 충전 (각각 다른 requestId)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    String requestId = UUID.randomUUID().toString();
                    PointRequest request = new PointRequest(requestId, chargeAmount, UseType.CHARGE);
                    PointResponse response = savePointPort.savePoint(testUserId, request);

                    successCount.incrementAndGet();
                    log.info("충전 성공: balance={}", response.balance());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("충전 실패: error={}", e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then: 모두 성공하고 최종 잔액은 10000
        log.info("성공: {}, 실패: {}", successCount.get(), failCount.get());
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);

        // DB 확인
        PointResponse finalBalance = transactionTemplate.execute(status ->
                getPointBalancePort.getPoint(testUserId));
        assertThat(finalBalance.balance()).isEqualTo(chargeAmount * threadCount);
    }

    @Test
    @DisplayName("동일 사용자가 동시에 포인트 사용 - 잔액 범위 내에서만 성공")
    void concurrentUse_onlyWithinBalance() throws InterruptedException {
        // given: 초기 잔액 5000 포인트
        long initialBalance = 5000L;
        String initRequestId = UUID.randomUUID().toString();
        savePointPort.savePoint(testUserId, new PointRequest(initRequestId, initialBalance, UseType.CHARGE));

        int threadCount = 10;
        long useAmount = 1000L; // 각각 1000씩 사용 시도
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 10명이 동시에 1000씩 사용 (총 10000 시도, 잔액은 5000)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    String requestId = UUID.randomUUID().toString();
                    PointRequest request = new PointRequest(requestId, useAmount, UseType.USE);
                    PointResponse response = savePointPort.savePoint(testUserId, request);

                    successCount.incrementAndGet();
                    log.info("사용 성공: balance={}", response.balance());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("사용 실패: error={}", e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then: 5명만 성공 (5000 / 1000 = 5)
        log.info("성공: {}, 실패: {}", successCount.get(), failCount.get());
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);

        // DB 확인: 최종 잔액 0
        PointResponse finalBalance = transactionTemplate.execute(status ->
                getPointBalancePort.getPoint(testUserId));
        assertThat(finalBalance.balance()).isEqualTo(0L);
    }

    @Test
    @DisplayName("충전과 사용이 동시에 발생 - 정합성 보장")
    void concurrentChargeAndUse_consistencyGuaranteed() throws InterruptedException {
        // given: 초기 잔액 1000
        long initialBalance = 1000L;
        String initRequestId = UUID.randomUUID().toString();
        savePointPort.savePoint(testUserId, new PointRequest(initRequestId, initialBalance, UseType.CHARGE));

        int chargeThreads = 5;
        int useThreads = 5;
        long chargeAmount = 500L;
        long useAmount = 300L;

        ExecutorService executorService = Executors.newFixedThreadPool(chargeThreads + useThreads);
        CountDownLatch latch = new CountDownLatch(chargeThreads + useThreads);

        AtomicInteger chargeSuccess = new AtomicInteger(0);
        AtomicInteger useSuccess = new AtomicInteger(0);
        AtomicInteger useFail = new AtomicInteger(0);

        // when: 충전 5번 (각 500) + 사용 5번 시도 (각 300)
        // 충전 스레드
        for (int i = 0; i < chargeThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    String requestId = UUID.randomUUID().toString();
                    PointRequest request = new PointRequest(requestId, chargeAmount, UseType.CHARGE);
                    savePointPort.savePoint(testUserId, request);
                    chargeSuccess.incrementAndGet();
                } catch (Exception e) {
                    log.error("충전 실패: error={}", e.getMessage());
                }
            });
        }

        // 사용 스레드
        for (int i = 0; i < useThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    String requestId = UUID.randomUUID().toString();
                    PointRequest request = new PointRequest(requestId, useAmount, UseType.USE);
                    savePointPort.savePoint(testUserId, request);
                    useSuccess.incrementAndGet();
                } catch (Exception e) {
                    useFail.incrementAndGet();
                    log.debug("사용 실패: error={}", e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then
        log.info("충전 성공: {}, 사용 성공: {}, 사용 실패: {}",
                chargeSuccess.get(), useSuccess.get(), useFail.get());

        // 충전은 모두 성공
        assertThat(chargeSuccess.get()).isEqualTo(chargeThreads);

        // DB 확인: 최종 잔액 = 초기(1000) + 충전(500*5) - 사용(300*성공횟수)
        PointResponse finalBalance = transactionTemplate.execute(status ->
                getPointBalancePort.getPoint(testUserId));
        long expectedBalance = initialBalance + (chargeAmount * chargeSuccess.get()) - (useAmount * useSuccess.get());
        assertThat(finalBalance.balance()).isEqualTo(expectedBalance);

        // 잔액이 음수가 아닌지 확인
        assertThat(finalBalance.balance()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("동일 requestId로 여러 번 충전 시도 - 멱등성 보장(한 번만 충전)")
    void concurrentChargeSameRequest_idempotency() throws InterruptedException {
        // given
        String requestId = UUID.randomUUID().toString();
        long chargeAmount = 1000L;
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<PointResponse> responses = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        // when: 10개 스레드가 동시에 같은 requestId로 충전 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    PointRequest request = new PointRequest(requestId, chargeAmount, UseType.CHARGE);
                    PointResponse response = savePointPort.savePoint(testUserId, request);

                    responses.add(response);
                    successCount.incrementAndGet();
                    log.info("충전 성공: balance={}", response.balance());
                } catch (Exception e) {
                    log.error("충전 실패: error={}", e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then: 모두 성공
        assertThat(successCount.get()).isEqualTo(threadCount);

        // 모든 응답이 동일한 잔액 (멱등성)
        assertThat(responses.stream().map(PointResponse::balance).distinct()).hasSize(1);
        assertThat(responses.get(0).balance()).isEqualTo(chargeAmount);

        // DB 확인: 최종 잔액은 1번만 충전됨
        PointResponse finalBalance = transactionTemplate.execute(status ->
                getPointBalancePort.getPoint(testUserId));
        assertThat(finalBalance.balance()).isEqualTo(chargeAmount);

    }

    @Test
    @DisplayName("동일 requestId로 동시 사용 - 멱등성 보장 (한 번만 차감)")
    void concurrentUseSameRequest_idempotency() throws InterruptedException {
        // given: 초기 잔액 10000
        long initialBalance = 10000L;
        String initRequestId = UUID.randomUUID().toString();
        savePointPort.savePoint(testUserId, new PointRequest(initRequestId, initialBalance, UseType.CHARGE));

        String useRequestId = UUID.randomUUID().toString();
        long useAmount = 3000L;

        // when: 첫 번째 사용
        PointRequest request = new PointRequest(useRequestId, useAmount, UseType.USE);
        PointResponse firstResponse = savePointPort.savePoint(testUserId, request);
        log.info("첫 번째 사용 완료: balance={}", firstResponse.balance());

        // then: 동일 requestId로 9번 재시도
        int retryCount = 9;
        ExecutorService executorService = Executors.newFixedThreadPool(retryCount);
        CountDownLatch latch = new CountDownLatch(retryCount);

        List<PointResponse> responses = new CopyOnWriteArrayList<>();
        responses.add(firstResponse);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < retryCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    PointResponse response = savePointPort.savePoint(testUserId, request);
                    responses.add(response);
                    successCount.incrementAndGet();
                    log.info("재시도 사용 성공: balance={}", response.balance());
                } catch (Exception e) {
                    log.error("재시도 사용 실패: error={}", e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then: 모든 재시도 성공
        log.info("총 성공: {}, 잔액 종류: {}", successCount.get() + 1, responses.stream().map(PointResponse::balance).distinct().count());
        assertThat(successCount.get()).isEqualTo(retryCount);

        // 모든 응답이 동일한 잔액 (멱등성)
        long expectedBalance = initialBalance - useAmount;
        assertThat(responses.stream().map(PointResponse::balance).distinct()).hasSize(1);
        assertThat(responses.get(0).balance()).isEqualTo(expectedBalance);

        // DB 확인: 최종 잔액은 1번만 차감됨
        PointResponse finalBalance = transactionTemplate.execute(status ->
                getPointBalancePort.getPoint(testUserId));
        assertThat(finalBalance.balance()).isEqualTo(expectedBalance);
    }
}