package com.gomdol.concert.payment.application;

import com.gomdol.concert.payments.application.port.in.PaymentCommand;
import com.gomdol.concert.payments.application.port.out.PaymentRepository;
import com.gomdol.concert.payments.application.usecase.PaymentUseCase;
import com.gomdol.concert.payments.domain.PaymentStatus;
import com.gomdol.concert.payments.domain.model.Payment;
import com.gomdol.concert.payments.infra.PaymentCodeGenerator;
import com.gomdol.concert.payments.presentation.dto.PaymentResponse;
import com.gomdol.concert.point.domain.event.PointRequestedEvent;
import com.gomdol.concert.point.domain.event.PointResponseEvent;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.model.Reservation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.gomdol.concert.common.FixedField.*;
import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static com.gomdol.concert.common.ReservationTestFixture.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private PaymentCodeGenerator codeGenerator;

    @InjectMocks
    private PaymentUseCase useCase;

    @Test
    public void 예약처리된_좌석의_결제를_성공한다() throws Exception {
        // given
        PaymentCommand command = new PaymentCommand(1L, FIXED_UUID, FIXED_REQUEST_ID,30000);
        Reservation reservation = mockReservation(mockReservationSeats());
        Payment payment = mockPayment();
        when(paymentRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(reservationRepository.findById(command.reservationId())).thenReturn(Optional.of(reservation));
        when(codeGenerator.newCodeGenerate()).thenReturn(PAYMENT_CODE);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        // when
        PaymentResponse result = useCase.processPayment(command);
        // then
        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo(1L);
        assertThat(result.reservationId()).isEqualTo(command.reservationId());
        assertThat(PaymentStatus.PENDING).isEqualTo(payment.getStatus());

        ArgumentCaptor<PointRequestedEvent> captor = ArgumentCaptor.forClass(PointRequestedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        PointRequestedEvent e = captor.getValue();

        assertThat(e.userId()).isEqualTo(FIXED_UUID);
        assertThat(e.requestId()).isEqualTo(FIXED_REQUEST_ID);
        assertThat(e.amount()).isEqualTo(30000L);
    }

    @Test
    public void 포인트_결제가_성공이면_결제를_마무리한다() throws Exception {
        // given
        PointResponseEvent event = PointResponseEvent.succeededEvent(FIXED_UUID, FIXED_REQUEST_ID, 30000);
        Payment payment = mockSuccessPayment();
        Reservation reservation = mockReservation(mockConfirmedReservationSeats());
        when(paymentRepository.findByRequestId(event.requestId())).thenReturn(Optional.of(mockPayment()));
        when(reservationRepository.findById(payment.getReservationId())).thenReturn(Optional.of(mockReservation(mockReservationSeats())));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        // when
        useCase.onPointResponse(event);
        // then
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation saved = captor.getValue();
        assertAll(
                () -> Assertions.assertThat(saved.getUserId()).isEqualTo(FIXED_UUID),
                () -> Assertions.assertThat(saved.getReservationCode()).isEqualTo(RESERVATION_CODE),
                () -> Assertions.assertThat(saved.getRequestId()).isEqualTo(FIXED_REQUEST_ID),
                () -> Assertions.assertThat(saved.allSeatsAreConfirmed()).isTrue()
        );
        verify(paymentRepository).save(argThat(save -> save.getStatus()==PaymentStatus.SUCCEEDED));
    }

    @Test
    public void 포인트_결제가_실패면_결제를_취소한다() throws Exception {
        // given
        PointResponseEvent event = PointResponseEvent.failedEvent(FIXED_UUID, FIXED_REQUEST_ID, "error",30000);
        Payment payment = mockFailedPayment();
        Reservation reservation = mockReservation(mockCanceledReservationSeats());
        when(paymentRepository.findByRequestId(event.requestId())).thenReturn(Optional.of(mockPayment()));
        when(reservationRepository.findById(payment.getReservationId())).thenReturn(Optional.of(mockReservation(mockReservationSeats())));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        // when
        useCase.onPointResponse(event);
        // then
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation saved = captor.getValue();
        assertAll(
                () -> Assertions.assertThat(saved.getUserId()).isEqualTo(FIXED_UUID),
                () -> Assertions.assertThat(saved.getReservationCode()).isEqualTo(RESERVATION_CODE),
                () -> Assertions.assertThat(saved.getRequestId()).isEqualTo(FIXED_REQUEST_ID),
                () -> Assertions.assertThat(saved.allSeatsAreCancel()).isTrue()
        );
        verify(paymentRepository).save(argThat(save -> save.getStatus() == PaymentStatus.FAILED));
    }

    @Test
    public void 예약이_존재하지_않으면_에러를_발생시킨다() throws Exception {
        // given
        PaymentCommand command = new PaymentCommand(1L, FIXED_UUID, FIXED_REQUEST_ID,30000);
        when(paymentRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(reservationRepository.findById(command.reservationId())).thenReturn(Optional.empty());
        // when &&  then
        assertThatThrownBy(() -> useCase.processPayment(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약을 찾을 수 없습니다");

    }

    @Test
    public void 만료된_예약_결제시_에러를_발생시킨다() throws Exception {
        // given
        PaymentCommand command = new PaymentCommand(1L, FIXED_UUID, FIXED_REQUEST_ID,30000);
        when(paymentRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(reservationRepository.findById(command.reservationId())).thenReturn(Optional.of(mockExpireReservation()));
        // when &&  then
        assertThatThrownBy(() -> useCase.processPayment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("만료");
    }

    @Test
    public void 이미_확정된_예약_결제시_에러를_발생시킨다() throws Exception {
        // given
        PaymentCommand command = new PaymentCommand(1L, FIXED_UUID, FIXED_REQUEST_ID,30000);
        when(paymentRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(reservationRepository.findById(command.reservationId())).thenReturn(Optional.of(mockOneSeatReservation(mockConfirmedOneReservationSeat())));
        // when &&  then
        assertThatThrownBy(() -> useCase.processPayment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("확정");
    }

    @Test
    public void 취소된_예약_결제시_에러를_발생시킨다() throws Exception {
        // given
        PaymentCommand command = new PaymentCommand(1L, FIXED_UUID, FIXED_REQUEST_ID,30000);
        when(paymentRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(reservationRepository.findById(command.reservationId())).thenReturn(Optional.of(mockOneSeatReservation(mockCanceledOneReservationSeat())));
        // when &&  then
        assertThatThrownBy(() -> useCase.processPayment(command))
                .isInstanceOf(IllegalStateException.class);
    }

//    @Test
//    public void 보유포인트가_결제금액보다_작으면_에러를_발생하고_결제_실패처리를_한다() throws Exception {
//        // given
//
//        // when
//
//        // then
//    }

    @Test
    public void 동일한_멱등키로_요청이들어오면_이미_완료된_결제라고_에러_발생시킨다() throws Exception {
        // given
        PaymentCommand command = new PaymentCommand(1L, FIXED_UUID, FIXED_REQUEST_ID,30000);
        Reservation reservation = mockReservation(mockReservationSeats());
        Payment payment = mockPayment();
        when(paymentRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty(), Optional.of(payment));
        when(reservationRepository.findById(command.reservationId())).thenReturn(Optional.of(reservation));
        when(codeGenerator.newCodeGenerate()).thenReturn(PAYMENT_CODE);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        // when
        PaymentResponse result = useCase.processPayment(command);
        PaymentResponse secondResult = useCase.processPayment(command);
        // then
        verify(paymentRepository, times(1)).save(any(Payment.class));

        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo(1L);
        assertThat(result.reservationId()).isEqualTo(command.reservationId());
        assertThat(PaymentStatus.PENDING).isEqualTo(payment.getStatus());

        assertThat(secondResult).isNotNull();
        assertThat(secondResult.paymentId()).isEqualTo(1L);
        assertThat(secondResult.reservationId()).isEqualTo(command.reservationId());
        assertThat(PaymentStatus.PENDING).isEqualTo(payment.getStatus());
    }

    private Payment mockPayment() {
        return Payment.of(
                1L,
                1L,
                FIXED_UUID,
                PAYMENT_CODE,
                FIXED_REQUEST_ID,
                30000,
                PaymentStatus.PENDING,
                null);
    }

    private Payment mockFailedPayment() {
        return Payment.of(
                1L,
                1L,
                FIXED_UUID,
                PAYMENT_CODE,
                FIXED_REQUEST_ID,
                30000,
                PaymentStatus.FAILED,
                LocalDateTime.now());
    }

    private Payment mockSuccessPayment() {
      return Payment.of(
                1L,
                1L,
                FIXED_UUID,
                PAYMENT_CODE,
                FIXED_REQUEST_ID,
                30000,
              PaymentStatus.SUCCEEDED,
              LocalDateTime.now());
    }


}
