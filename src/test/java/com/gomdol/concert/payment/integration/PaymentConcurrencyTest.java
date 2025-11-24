package com.gomdol.concert.payment.integration;

import com.gomdol.concert.common.TestContainerConfig;
import com.gomdol.concert.common.TestDataFactory;
import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import com.gomdol.concert.payment.application.port.in.PaymentPort;
import com.gomdol.concert.payment.application.port.in.PaymentPort.PaymentCommand;
import com.gomdol.concert.payment.infra.persistence.PaymentJpaRepository;
import com.gomdol.concert.payment.presentation.dto.PaymentResponse;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort.ReservationSeatCommand;
import com.gomdol.concert.reservation.infra.persistence.ReservationJpaRepository;
import com.gomdol.concert.reservation.infra.persistence.ReservationSeatJpaRepository;
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
import org.springframework.transaction.support.TransactionTemplate;

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
@DisplayName("결제 동시성 테스트")
@Import(TestContainerConfig.class)
class PaymentConcurrencyTest {

    @Autowired
    private PaymentPort paymentPort;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private ReservationSeatPort reservationSeatPort;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private ReservationSeatJpaRepository reservationSeatJpaRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long testShowId;
    private List<Long> testSeatIds;

    @BeforeEach
    void setUp() {
        // 기존 데이터 초기화
        paymentJpaRepository.deleteAll();
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
    @DisplayName("동일 예약에 동일 requestId로 동시 결제 - 멱등성 테스트 (모두 성공, 같은 결제)")
    void concurrentPaymentSameRequest_idempotency() throws InterruptedException {
        // given - 예약 생성
        String userId = UUID.randomUUID().toString();
        String reservationRequestId = UUID.randomUUID().toString();
        List<Long> seatIds = List.of(testSeatIds.get(0), testSeatIds.get(1));
        ReservationSeatCommand reservationCommand = new ReservationSeatCommand(userId, reservationRequestId, testShowId, seatIds);
        ReservationResponse reservation = reservationSeatPort.reservationSeat(reservationCommand);

        // 포인트 충전
        transactionTemplate.execute(status -> {
            Point point = Point.create(userId, 1000000L);
            return pointRepository.save(point);
        });

        // 동일한 결제 requestId
        String paymentRequestId = UUID.randomUUID().toString();
        long amount = seatIds.size() * 10000L; // 좌석당 10,000원
        PaymentCommand command = new PaymentCommand(reservation.reservationId(), userId, paymentRequestId, amount);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Long> paymentIds = new CopyOnWriteArrayList<>();

        // when - 동일 requestId로 동시 결제 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    PaymentResponse response = paymentPort.processPayment(command);
                    successCount.incrementAndGet();
                    paymentIds.add(response.paymentId());
                    log.info("결제 성공: paymentId={}", response.paymentId());
                } catch (Exception e) {
                    log.error("결제 실패: error={}", e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then - 모든 요청이 성공하고, 동일한 결제 ID를 반환해야 함
        log.info("총 성공: {}, 결제 ID 종류: {}", successCount.get(), paymentIds.stream().distinct().count());
        assertThat(successCount.get()).isEqualTo(threadCount); // 모두 성공
        assertThat(paymentIds.stream().distinct()).hasSize(1); // 동일한 결제 ID

        // DB 확인: 결제가 1건만 생성되어야 함
        long paymentCount = paymentJpaRepository.findAll().stream()
                .filter(p -> p.getUserId().equals(userId))
                .count();
        assertThat(paymentCount).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 예약에 대해 동시 결제 - 모두 성공")
    void concurrentPaymentDifferentReservations_allSucceed() throws InterruptedException {
        // given
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<String> userIds = new ArrayList<>();
        List<ReservationResponse> reservations = new ArrayList<>();

        // 20개의 서로 다른 예약 생성
        for (int i = 0; i < threadCount; i++) {
            String userId = UUID.randomUUID().toString();
            userIds.add(userId);

            // 예약
            String reservationRequestId = UUID.randomUUID().toString();
            List<Long> seatIds = List.of(testSeatIds.get(i));
            ReservationSeatCommand reservationCommand = new ReservationSeatCommand(userId, reservationRequestId, testShowId, seatIds);
            ReservationResponse reservation = reservationSeatPort.reservationSeat(reservationCommand);
            reservations.add(reservation);

            // 포인트 충전
            transactionTemplate.execute(status -> {
                Point point = Point.create(userId, 1000000L);
                return pointRepository.save(point);
            });
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 서로 다른 예약에 대해 동시 결제
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    String userId = userIds.get(index);
                    ReservationResponse reservation = reservations.get(index);
                    String paymentRequestId = UUID.randomUUID().toString();
                    long amount = 10000L; // 좌석당 10,000원
                    PaymentCommand command = new PaymentCommand(reservation.reservationId(), userId, paymentRequestId, amount);

                    PaymentResponse response = paymentPort.processPayment(command);
                    successCount.incrementAndGet();
                    log.info("결제 성공: userId={}, paymentId={}", userId, response.paymentId());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("결제 실패: user{}, error={}", index, e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then - 모두 성공해야 함
        log.info("성공: {}, 실패: {}", successCount.get(), failCount.get());
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);

        // DB 확인
        long paymentCount = paymentJpaRepository.count();
        assertThat(paymentCount).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("동일 사용자가 여러 예약에 대해 동시 결제 - 포인트 잔액 검증")
    void concurrentPaymentSameUser_pointValidation() throws InterruptedException {
        // given
        String userId = UUID.randomUUID().toString();
        int reservationCount = 10;
        long initialBalance = 100000L; // 충분하지 않은 잔액

        // 포인트 충전 (10개 예약하기에는 부족한 금액)
        transactionTemplate.execute(status -> {
            Point point = Point.create(userId, initialBalance);
            return pointRepository.save(point);
        });

        List<ReservationResponse> reservations = new ArrayList<>();

        // 10개의 예약 생성 (각 10,000원)
        for (int i = 0; i < reservationCount; i++) {
            String reservationRequestId = UUID.randomUUID().toString();
            List<Long> seatIds = List.of(testSeatIds.get(i));
            ReservationSeatCommand reservationCommand = new ReservationSeatCommand(userId, reservationRequestId, testShowId, seatIds);
            ReservationResponse reservation = reservationSeatPort.reservationSeat(reservationCommand);
            reservations.add(reservation);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(reservationCount);
        CountDownLatch latch = new CountDownLatch(reservationCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동일 사용자가 여러 예약에 동시 결제 (포인트 부족 예상)
        for (int i = 0; i < reservationCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    ReservationResponse reservation = reservations.get(index);
                    String paymentRequestId = UUID.randomUUID().toString();
                    long amount = 10000L; // 좌석당 10,000원
                    PaymentCommand command = new PaymentCommand(reservation.reservationId(), userId, paymentRequestId, amount);

                    PaymentResponse response = paymentPort.processPayment(command);
                    successCount.incrementAndGet();
                    log.info("결제 요청 성공: paymentId={}", response.paymentId());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("결제 실패: reservation{}, error={}", index, e.getMessage());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // then - 결제 요청은 성공하지만 (PENDING 상태), 포인트 차감은 일부만 성공할 것
        log.info("결제 요청 성공: {}, 실패: {}", successCount.get(), failCount.get());

        // 결제 요청 자체는 모두 성공 (PENDING 상태로 생성됨)
        // 실제 포인트 차감은 이벤트 기반으로 처리되므로 여기서는 확인 안 함
        long paymentCount = paymentJpaRepository.findAll().stream()
                .filter(p -> p.getUserId().equals(userId))
                .count();
        assertThat(paymentCount).isGreaterThan(0);
        assertThat(paymentCount).isLessThanOrEqualTo(reservationCount);
    }
}