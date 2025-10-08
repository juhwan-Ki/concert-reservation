package com.gomdol.concert.payment.application.usecase;

import com.gomdol.concert.payment.application.port.in.PaymentCommand;
import com.gomdol.concert.payment.application.port.out.PaymentRepository;
import com.gomdol.concert.payment.domain.model.Payment;
import com.gomdol.concert.payment.infra.PaymentCodeGenerator;
import com.gomdol.concert.payment.presentation.dto.PaymentResponse;
import com.gomdol.concert.point.domain.event.PointRequestedEvent;
import com.gomdol.concert.point.domain.event.PointResponseEvent;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentCodeGenerator codeGenerator;

    @Transactional
    public PaymentResponse processPayment(PaymentCommand command) {
        log.info("payment request: {}", command);
        // 멱등성 체크
        Optional<Payment> existed = paymentRepository.findByRequestId(command.requestId());
        if (existed.isPresent())
            return PaymentResponse.fromDomain(existed.get());
        // 결제 준비
        if(command.amount() <= 0)
            throw new IllegalArgumentException("결제 금액은 0보다 커야합니다");
        Reservation reservation = getPaymentEligibleReservation(command.reservationId(), command.amount());
        Payment saved = paymentRepository.save(Payment.create(
                reservation.getId(),
                command.userId(),
                codeGenerator.newCodeGenerate(),
                command.requestId(),
                reservation.getAmount()));

        eventPublisher.publishEvent(PointRequestedEvent.useRequest(
                command.userId(),
                command.requestId(),
                command.amount()
                ));

        return PaymentResponse.fromDomain(saved);
    }

    private Reservation getPaymentEligibleReservation(Long reservationId, long amount) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다. ID: " + reservationId));
        reservation.validateAllSeatsPaymentEligible();
        if(reservation.getAmount() != amount)
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다. 예약 금액: " + reservation.getAmount() + " 결제 금액: " + amount);

        return reservation;
    }

    // 포인트 처리 결과 수신 → 좌석 확정/보상 + 결제 상태 전이
    @Transactional
    @EventListener
    public void onPointResponse(PointResponseEvent event) {
        try {
            Payment payment = paymentRepository.findByRequestId(event.requestId())
                    .orElseThrow(() -> new IllegalStateException("결제 내역을 찾을 수 없습니다: " + event.requestId()));

            // 이미 최종 상태면 무시
            if (payment.isTerminal()) {
                log.debug("이미 진행된 결제입니다. requestId={}", event.requestId());
                return;
            }

            Reservation reservation = getPaymentEligibleReservation(payment.getReservationId(), event.amount());
            if (event.isSucceeded()) {
                // 좌석 확정
                reservation.confirmSeats();
                reservationRepository.save(reservation);
                // 결제 확정
                payment.succeed();
                paymentRepository.save(payment);
                log.info("결제 완료: paymentId={}", payment.getId());
            } else {
                // 보상 트랜잭션
                log.warn("결제 실패 - 보상 시작: requestId={}", event.requestId());
                // 좌석 취소
                reservation.cancelSeats();
                reservationRepository.save(reservation);
                // 결제 취소
                payment.failed();
                paymentRepository.save(payment);
                log.info("보상 완료: 예약 취소됨");
            }
        } catch (Exception e) {
            log.error("결제 처리 실패: requestId={}", event.requestId(), e);
//            alertService.sendAlert(...); // TODO: 추후 슬랙 알람 같은거 추가하면 좋을듯
            throw e;
        }
    }
}
