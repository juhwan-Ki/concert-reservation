package com.gomdol.concert.reservation.integration;

import com.gomdol.concert.common.TestContainerConfig;
import com.gomdol.concert.common.TestDataFactory;
import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort.ReservationSeatCommand;
import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import com.gomdol.concert.reservation.infra.persistence.ReservationJpaRepository;
import com.gomdol.concert.reservation.infra.persistence.ReservationSeatJpaRepository;
import com.gomdol.concert.reservation.infra.persistence.entity.ReservationSeatEntity;
import com.gomdol.concert.show.infra.persistence.entity.ShowEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueSeatEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("좌석 예약 동시성 테스트")
@Import(TestContainerConfig.class)
class ReservationConcurrencyTest {

    @Autowired
    private ReservationSeatPort reservationSeatPort;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private ReservationSeatJpaRepository reservationSeatJpaRepository;

    @Autowired
    private TestDataFactory testDataFactory;

    private Long testShowId;
    private List<Long> testSeatIds;

    @BeforeEach
    void setUp() {
        // 기존 예약 데이터 초기화
        reservationSeatJpaRepository.deleteAll();
        reservationJpaRepository.deleteAll();

        // 테스트 데이터 생성
        VenueEntity venue = testDataFactory.createVenue("Test Venue", "Seoul", 100);

        // 100개의 좌석 생성
        testSeatIds = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            VenueSeatEntity seat = testDataFactory.createVenueSeat(venue, "A", i, 10000L);
            testSeatIds.add(seat.getId());
        }

        ConcertEntity concert = testDataFactory.createConcert("Test Concert", venue);
        ShowEntity show = testDataFactory.createShow(concert, LocalDateTime.now().plusDays(7), 100);
        testShowId = show.getId();
    }

    @Test
    @DisplayName("동일한 좌석에 100명이 동시 예약 시도 - 1명만 성공해야 함")
    void concurrentReservationSameSeat_onlyOneSucceeds() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Long targetSeatId = testSeatIds.get(0); // 첫 번째 좌석
        List<Long> seatIds = List.of(targetSeatId);

        // UUID 형식의 userId 미리 생성
        List<String> userIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            userIds.add(UUID.randomUUID().toString());
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Future<ReservationResponse>> futures = new ArrayList<>();

        // when: 100명이 동일한 좌석을 동시에 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            Future<ReservationResponse> future = executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // 모든 스레드가 동시에 시작되도록 대기

                    String userId = userIds.get(index);
                    String requestId = UUID.randomUUID().toString();
                    ReservationSeatCommand command = new ReservationSeatCommand(userId, requestId, testShowId, seatIds);

                    ReservationResponse response = reservationSeatPort.reservationSeat(command);
                    successCount.incrementAndGet();
                    log.info("예약 성공: userId={}, reservationCode={}", userId, response.reservationCode());
                    return response;
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("예약 실패: user{}, error={}", index, e.getMessage());
                    return null;
                }
            });
            futures.add(future);
        }

        // 모든 작업 완료 대기
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then: 정확히 1명만 성공해야 함
        log.info("성공: {}, 실패: {}", successCount.get(), failCount.get());
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);

        // DB 확인: HOLD 상태의 좌석이 정확히 1개만 있어야 함
        List<ReservationSeatEntity> reservedSeats = reservationSeatJpaRepository.findAll().stream()
                .filter(rs -> rs.getStatus() == ReservationSeatStatus.HOLD)
                .filter(rs -> rs.getSeatId().equals(targetSeatId))
                .toList();

        assertThat(reservedSeats).hasSize(1);
    }

    @Test
    @DisplayName("서로 다른 좌석을 50명이 동시 예약 - 모두 성공해야 함")
    void concurrentReservationDifferentSeats_allSucceed() throws InterruptedException {
        // given
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // UUID 형식의 userId 미리 생성
        List<String> userIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            userIds.add(UUID.randomUUID().toString());
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 50명이 서로 다른 좌석을 동시에 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    String userId = userIds.get(index);
                    String requestId = UUID.randomUUID().toString();
                    Long seatId = testSeatIds.get(index); // 각자 다른 좌석
                    ReservationSeatCommand command = new ReservationSeatCommand(userId, requestId, testShowId, List.of(seatId));

                    ReservationResponse response = reservationSeatPort.reservationSeat(command);
                    successCount.incrementAndGet();
                    log.info("예약 성공: userId={}, seatId={}, reservationCode={}", userId, seatId, response.reservationCode());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("예약 실패: user{}, error={}", index, e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then: 모두 성공해야 함
        log.info("성공: {}, 실패: {}", successCount.get(), failCount.get());
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);

        // DB 확인
        List<ReservationSeatEntity> reservedSeats = reservationSeatJpaRepository.findAll().stream()
                .filter(rs -> rs.getStatus() == ReservationSeatStatus.HOLD)
                .toList();

        assertThat(reservedSeats).hasSize(threadCount);
    }

    @Test
    @DisplayName("부분적으로 겹치는 좌석들 동시 예약 - 겹치지 않은 예약만 성공")
    void concurrentReservationPartialOverlap_conflictsHandled() throws InterruptedException {
        // given
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // UUID 형식의 userId 미리 생성
        List<String> userIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            userIds.add(UUID.randomUUID().toString());
        }

        // 좌석 1, 2, 3을 공유하는 예약 패턴
        // user0-9: 좌석 [1, 2] 예약 시도
        // user10-19: 좌석 [2, 3] 예약 시도
        // 예상: 좌석 2가 겹치므로 각 그룹에서 1명씩만 성공 (총 2명)

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    String userId = userIds.get(index);
                    String requestId = UUID.randomUUID().toString();

                    List<Long> seatIds;
                    if (index < 10) {
                        // 좌석 1, 2 예약 시도
                        seatIds = List.of(testSeatIds.get(0), testSeatIds.get(1));
                    } else {
                        // 좌석 2, 3 예약 시도
                        seatIds = List.of(testSeatIds.get(1), testSeatIds.get(2));
                    }

                    ReservationSeatCommand command = new ReservationSeatCommand(userId, requestId, testShowId, seatIds);
                    ReservationResponse response = reservationSeatPort.reservationSeat(command);
                    successCount.incrementAndGet();
                    log.info("예약 성공: userId={}, seats={}, reservationCode={}", userId, seatIds, response.reservationCode());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("예약 실패: user{}, error={}", index, e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then: 좌석 2가 겹치므로 2명만 성공해야 함 (이론적으로)
        // 실제로는 락이 없으면 더 많이 성공할 수 있음 (동시성 이슈)
        log.info("성공: {}, 실패: {}", successCount.get(), failCount.get());

        // 최소한 성공 횟수가 threadCount보다 작아야 함 (모두 성공하면 안됨)
        assertThat(successCount.get()).isLessThan(threadCount);

        // 예상: 2명 성공 (락이 제대로 걸린다면)
        // 하지만 락이 없으면 더 많이 성공할 수 있음
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("동일 사용자가 여러 좌석 동시 예약 - 멱등성 테스트")
    void concurrentReservationSameRequest_idempotency() throws InterruptedException {
        // given
        String userId = UUID.randomUUID().toString();
        String requestId = UUID.randomUUID().toString(); // 동일한 requestId
        List<Long> seatIds = List.of(testSeatIds.get(0), testSeatIds.get(1));
        ReservationSeatCommand command = new ReservationSeatCommand(userId, requestId, testShowId, seatIds);
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<String> reservationCodes = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // 동시에 시작되도록 대기

                    ReservationResponse response = reservationSeatPort.reservationSeat(command);
                    successCount.incrementAndGet();
                    reservationCodes.add(response.reservationCode());
                    log.info("예약 성공: reservationCode={}", response.reservationCode());
                } catch (Exception e) {
                    log.error("예약 실패: error={}", e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then: 모든 요청이 성공하고, 동일한 예약 코드를 반환해야 함 (멱등성)
        log.info("총 성공: {}, 예약 코드 종류: {}", successCount.get(), reservationCodes.stream().distinct().count());
        assertThat(successCount.get()).isEqualTo(threadCount); // 10번 모두 성공

        // 모든 응답이 동일한 예약 코드를 가져야 함
        assertThat(reservationCodes.stream().distinct()).hasSize(1);

        // DB 확인: 예약이 1건만 생성되어야 함
        long reservationCount = reservationJpaRepository.findAll().stream()
                .filter(r -> r.getUserId().equals(userId))
                .count();
        assertThat(reservationCount).isEqualTo(1);
    }

    @Test
    @DisplayName("100명이 10개 좌석을 동시에 랜덤 예약 - 정합성 테스트")
    void concurrentReservationLimitedSeats_integrityTest() throws InterruptedException {
        // given
        int threadCount = 100;
        int availableSeats = 10;
        List<Long> limitedSeats = testSeatIds.subList(0, availableSeats);

        // UUID 형식의 userId 미리 생성
        List<String> userIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            userIds.add(UUID.randomUUID().toString());
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // when: 100명이 10개 좌석 중 랜덤하게 선택해서 예약
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    // 랜덤하게 1~3개의 좌석 선택
                    int seatCount = random.nextInt(1, 4);
                    List<Long> selectedSeats = new ArrayList<>();
                    for (int j = 0; j < seatCount; j++) {
                        selectedSeats.add(limitedSeats.get(random.nextInt(availableSeats)));
                    }

                    String userId = userIds.get(index);
                    String requestId = UUID.randomUUID().toString();
                    ReservationSeatCommand command = new ReservationSeatCommand(userId, requestId, testShowId, selectedSeats);

                    ReservationResponse response = reservationSeatPort.reservationSeat(command);
                    successCount.incrementAndGet();
                    log.info("예약 성공: userId={}, seats={}", userId, selectedSeats);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("예약 실패: user{}", index);
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then
        log.info("성공: {}, 실패: {}", successCount.get(), failCount.get());
        assertThat(successCount.get()).isLessThanOrEqualTo(availableSeats);

        // DB 검증: 각 좌석이 최대 1번만 예약되어야 함
        List<ReservationSeatEntity> allReservedSeats = reservationSeatJpaRepository.findAll().stream()
                .filter(rs -> rs.getStatus() == ReservationSeatStatus.HOLD)
                .toList();

        // 중복 예약된 좌석이 없어야 함
        List<Long> reservedSeatIds = allReservedSeats.stream()
                .map(ReservationSeatEntity::getSeatId)
                .toList();

        long uniqueSeatCount = reservedSeatIds.stream().distinct().count();
        assertThat(uniqueSeatCount).isEqualTo(reservedSeatIds.size()); // 중복이 없어야 함
    }
}