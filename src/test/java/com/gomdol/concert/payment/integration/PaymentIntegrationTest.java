package com.gomdol.concert.payment.integration;

import com.gomdol.concert.common.TestContainerConfig;
import com.gomdol.concert.common.TestDataFactory;
import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import com.gomdol.concert.payment.application.facade.PaymentFacade;
import com.gomdol.concert.payment.application.port.in.SavePaymentPort.PaymentCommand;
import com.gomdol.concert.payment.application.port.out.PaymentRepository;
import com.gomdol.concert.payment.domain.PaymentStatus;
import com.gomdol.concert.payment.domain.model.Payment;
import com.gomdol.concert.payment.infra.persistence.PaymentJpaRepository;
import com.gomdol.concert.payment.presentation.dto.PaymentResponse;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.domain.event.PointResponseEvent;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort.ReservationSeatCommand;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.infra.persistence.ReservationJpaRepository;
import com.gomdol.concert.reservation.infra.persistence.ReservationSeatJpaRepository;
import com.gomdol.concert.show.infra.persistence.entity.ShowEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueSeatEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("결제 통합 테스트")
@Import(TestContainerConfig.class)
class PaymentIntegrationTest {

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private ReservationSeatPort reservationSeatPort;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private ReservationSeatJpaRepository reservationSeatJpaRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private EntityManager entityManager;

    private Long testShowId;
    private Long testSeatId1;
    private Long testSeatId2;
    private Long reservationId;
    private long reservationAmount;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        paymentJpaRepository.deleteAll();
        reservationSeatJpaRepository.deleteAll();
        reservationJpaRepository.deleteAll();

        // 테스트 데이터 생성
        VenueEntity venue = testDataFactory.createVenue("Test Venue", "Seoul", 100);
        VenueSeatEntity seat1 = testDataFactory.createVenueSeat(venue, "A", 1, 50000L);
        VenueSeatEntity seat2 = testDataFactory.createVenueSeat(venue, "A", 2, 50000L);

        ConcertEntity concert = testDataFactory.createConcert("Test Concert", venue);
        ShowEntity show = testDataFactory.createShow(concert, LocalDateTime.now().plusDays(7), 100);

        testShowId = show.getId();
        testSeatId1 = seat1.getId();
        testSeatId2 = seat2.getId();

        // 예약 생성
        String reservationRequestId = UUID.randomUUID().toString();
        List<Long> seatIds = List.of(testSeatId1, testSeatId2);
        ReservationSeatCommand reservationCommand = new ReservationSeatCommand(FIXED_UUID, reservationRequestId, testShowId, seatIds);
        var reservationResponse = reservationSeatPort.reservationSeat(reservationCommand);
        reservationId = reservationResponse.reservationId();

        // 예약 금액 조회
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        reservationAmount = reservation.getAmount();

        // 포인트 충전 (결제를 위한 잔액)
        Point point = pointRepository.findByUserIdWithLock(FIXED_UUID)
                .orElseGet(() -> Point.create(FIXED_UUID, 0L));
        point.changeBalance(200000L);
        pointRepository.save(point);
    }

    @Test
    @DisplayName("결제 성공 - PENDING 상태로 생성")
    void 결제_요청_성공() {
        // given
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(reservationId, FIXED_UUID, requestId, reservationAmount);

        // when
        PaymentResponse response = paymentFacade.processPayment(command);

        // then
        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isNotNull();
        assertThat(response.reservationId()).isEqualTo(reservationId);
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING.name());
        assertThat(response.amount()).isEqualTo(reservationAmount);
        assertThat(response.paidAt()).isNull(); // PENDING 상태에서는 null

        // DB 확인
        Payment savedPayment = paymentRepository.findByRequestId(requestId).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getUserId()).isEqualTo(FIXED_UUID);
    }

    @Test
    @DisplayName("멱등성 테스트 - 동일한 requestId로 재요청 시 기존 결제 반환")
    void 멱등성_테스트() {
        // given
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(reservationId, FIXED_UUID, requestId, reservationAmount);

        // when - 첫 번째 결제
        PaymentResponse firstResponse = paymentFacade.processPayment(command);

        // when - 동일한 requestId로 재요청
        PaymentResponse secondResponse = paymentFacade.processPayment(command);

        // then - 동일한 결제가 반환되어야 함
        assertThat(firstResponse.paymentId()).isEqualTo(secondResponse.paymentId());
        assertThat(firstResponse.reservationId()).isEqualTo(secondResponse.reservationId());
        assertThat(firstResponse.status()).isEqualTo(secondResponse.status());

        // Repository로 조회하여 같은 결제임을 확인
        Payment payment = paymentRepository.findByRequestId(requestId).orElseThrow();
        assertThat(payment.getId()).isEqualTo(firstResponse.paymentId());
        assertThat(payment.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("존재하지 않는 예약 ID로 결제 시 예외 발생")
    void 존재하지_않는_예약_결제_실패() {
        // given
        Long nonExistentReservationId = 99999L;
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(nonExistentReservationId, FIXED_UUID, requestId, 100000L);

        // when & then
        assertThatThrownBy(() -> paymentFacade.processPayment(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("결제 금액이 예약 금액과 다를 때 예외 발생")
    void 금액_불일치_결제_실패() {
        // given
        String requestId = UUID.randomUUID().toString();
        long wrongAmount = reservationAmount + 10000L; // 다른 금액
        PaymentCommand command = new PaymentCommand(reservationId, FIXED_UUID, requestId, wrongAmount);

        // when & then
        assertThatThrownBy(() -> paymentFacade.processPayment(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("결제 금액이 일치하지 않습니다");
    }

    @Test
    @DisplayName("결제 금액이 0 이하일 때 예외 발생")
    void 잘못된_금액_결제_실패() {
        // given
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(reservationId, FIXED_UUID, requestId, 0L);

        // when & then
        assertThatThrownBy(() -> paymentFacade.processPayment(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("결제 금액은 0보다 커야합니다");
    }

    @Test
    @DisplayName("포인트 사용 성공 이벤트 수신 시 결제 완료 및 좌석 확정")
    void 포인트_사용_성공_결제_완료() {
        // given - 결제 요청
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(reservationId, FIXED_UUID, requestId, reservationAmount);
        PaymentResponse paymentResponse = paymentFacade.processPayment(command);

        // 트랜잭션 커밋을 위해 flush
        entityManager.flush();
        entityManager.clear();

        // when - 포인트 사용 성공 이벤트 발행
        PointResponseEvent successEvent = PointResponseEvent.succeededEvent(FIXED_UUID, requestId, reservationAmount);
        eventPublisher.publishEvent(successEvent);

        // 트랜잭션 커밋
        entityManager.flush();
        entityManager.clear();

        // then - 결제 상태가 SUCCEEDED로 변경
        Payment payment = paymentRepository.findByRequestId(requestId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.isSucceeded()).isTrue();

        // 예약 좌석 상태가 CONFIRMED로 변경
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        reservation.getReservationSeats().forEach(seat -> {
            assertThat(seat.getStatus()).isEqualTo(ReservationSeatStatus.CONFIRMED);
        });
    }

    @Test
    @DisplayName("포인트 사용 실패 이벤트 수신 시 결제 실패 및 좌석 취소")
    void 포인트_사용_실패_보상_처리() {
        // given - 결제 요청
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(reservationId, FIXED_UUID, requestId, reservationAmount);
        PaymentResponse paymentResponse = paymentFacade.processPayment(command);

        // 트랜잭션 커밋을 위해 flush
        entityManager.flush();
        entityManager.clear();

        // when - 포인트 사용 실패 이벤트 발행
        PointResponseEvent failedEvent = PointResponseEvent.failedEvent(FIXED_UUID, requestId, "포인트 잔액 부족", reservationAmount);
        eventPublisher.publishEvent(failedEvent);

        // 트랜잭션 커밋
        entityManager.flush();
        entityManager.clear();

        // then - 결제 상태가 FAILED로 변경
        Payment payment = paymentRepository.findByRequestId(requestId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaidAt()).isNull();
        assertThat(payment.isSucceeded()).isFalse();

        // 예약 좌석 상태가 CANCELED로 변경
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        reservation.getReservationSeats().forEach(seat -> {
            assertThat(seat.getStatus()).isEqualTo(ReservationSeatStatus.CANCELED);
        });
    }

    @Test
    @DisplayName("이미 완료된 결제에 대한 이벤트는 무시")
    void 완료된_결제_이벤트_무시() {
        // given - 결제 요청 및 완료
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(reservationId, FIXED_UUID, requestId, reservationAmount);
        paymentFacade.processPayment(command);

        // 첫 번째 성공 이벤트
        entityManager.flush();
        entityManager.clear();
        PointResponseEvent firstEvent = PointResponseEvent.succeededEvent(FIXED_UUID, requestId, reservationAmount);
        eventPublisher.publishEvent(firstEvent);
        entityManager.flush();
        entityManager.clear();

        Payment paymentAfterFirst = paymentRepository.findByRequestId(requestId).orElseThrow();
        assertThat(paymentAfterFirst.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        LocalDateTime firstPaidAt = paymentAfterFirst.getPaidAt();

        // when - 두 번째 이벤트 (중복)
        PointResponseEvent secondEvent = PointResponseEvent.succeededEvent(FIXED_UUID, requestId, reservationAmount);
        eventPublisher.publishEvent(secondEvent);
        entityManager.flush();
        entityManager.clear();

        // then - 결제 상태 변경 없음
        Payment paymentAfterSecond = paymentRepository.findByRequestId(requestId).orElseThrow();
        assertThat(paymentAfterSecond.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(paymentAfterSecond.getPaidAt()).isEqualTo(firstPaidAt);
    }

    @Test
    @DisplayName("결제 조회")
    void 결제_조회() {
        // given
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(reservationId, FIXED_UUID, requestId, reservationAmount);
        PaymentResponse createdPayment = paymentFacade.processPayment(command);

        // when
        Payment foundPayment = paymentRepository.findByRequestId(requestId).orElseThrow();

        // then
        assertThat(foundPayment).isNotNull();
        assertThat(foundPayment.getUserId()).isEqualTo(FIXED_UUID);
        assertThat(foundPayment.getReservationId()).isEqualTo(reservationId);
        assertThat(foundPayment.getAmount()).isEqualTo(reservationAmount);
        assertThat(foundPayment.getRequestId()).isEqualTo(requestId);
    }
}