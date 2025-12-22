package com.gomdol.concert.payment.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gomdol.concert.common.application.idempotency.port.in.CreateIdempotencyKey;
import com.gomdol.concert.common.application.outbox.OutboxRepository;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.common.domain.outbox.OutboxEvent;
import com.gomdol.concert.payment.application.port.in.SavePaymentPort;
import com.gomdol.concert.payment.application.port.out.PaymentRepository;
import com.gomdol.concert.payment.domain.command.UsePointCommand;
import com.gomdol.concert.payment.domain.model.Payment;
import com.gomdol.concert.payment.infra.PaymentCodeGenerator;
import com.gomdol.concert.payment.presentation.dto.PaymentResponse;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.gomdol.concert.common.domain.outbox.OutboxEvent.create;


/**
 * PaymentSaveUseCase
 * === Saga 흐름 ===
 * [정상 흐름]
 * 1. Payment 생성 (PENDING) + UsePointCommand Outbox 저장
 * 2. OutboxScheduler → Kafka: UsePointCommand
 * 3. Point Service → 포인트 차감 → PointUsedEvent(success) Outbox 저장
 * 4. OutboxScheduler → Kafka: PointUsedEvent
 * 5. Payment Consumer → Payment 상태 업데이트 (PROCESSING) + ConfirmSeatsCommand Outbox 저장
 * 6. OutboxScheduler → Kafka: ConfirmSeatsCommand
 * 7. Reservation Service → 좌석 확정 → SeatsConfirmedEvent Outbox 저장
 * 8. OutboxScheduler → Kafka: SeatsConfirmedEvent
 * 9. Payment Consumer → Payment 상태 업데이트 (COMPLETED)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSaveUseCase implements SavePaymentPort {

    private final CreateIdempotencyKey createIdempotencyKey;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentCodeGenerator codeGenerator;

    @Qualifier("paymentOutboxRepositoryImpl")
    private final OutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${kafka.topics.use-point-command:use-point-command}")
    private String usePointCommandTopic;

    /**
     * 결제 준비 (PENDING 상태로 생성)
     */
    @Transactional
    public PaymentResponse processPayment(SavePaymentPort.PaymentCommand command) {
        log.info("=== [Payment Service] Saga 시작 - requestId={} ===", command.requestId());

        // 결제 준비
        if(command.amount() <= 0)
            throw new IllegalArgumentException("결제 금액은 0보다 커야합니다");
        Reservation reservation = getPaymentEligibleReservation(command.reservationId(), command.amount());
        Payment payment = paymentRepository.save(Payment.create(
                reservation.getId(),
                command.userId(),
                codeGenerator.newCodeGenerate(),
                command.requestId(),
                reservation.getAmount()));

        // 멱등성 키 저장 - 성공적으로 처리된 요청 기록
        createIdempotencyKey.createIdempotencyKey(command.requestId(), command.userId(), ResourceType.PAYMENT, payment.getId());

        // UsePointCommand를 Payment Outbox에 저장
        UsePointCommand usePointCommand = UsePointCommand.of(payment.getId(), command.reservationId(), command.userId(), command.requestId(), command.amount());
        saveOutboxEvent(payment.getId().toString(), usePointCommandTopic, usePointCommand);
        log.info("[Payment Service] UsePointCommand Outbox 저장 완료 - paymentId={}", payment.getId());

        return PaymentResponse.fromDomain(payment);
    }

    private Reservation getPaymentEligibleReservation(Long reservationId, long amount) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다. ID: " + reservationId));
        reservation.validateAllSeatsPaymentEligible();
        if(reservation.getAmount() != amount)
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다. 예약 금액: " + reservation.getAmount() + " 결제 금액: " + amount);

        return reservation;
    }

    private void saveOutboxEvent(String aggregateId, String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = create("PAYMENT", aggregateId, "UsePointCommand", topic, payload);
            paymentOutboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("[Payment Outbox] 이벤트 직렬화 실패", e);
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }

    /**
     * 결제 취소 (보상 트랜잭션)
     */
    @Transactional
    public void cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다. ID: " + paymentId));

        payment.failed();
        paymentRepository.save(payment);

        log.info("결제 취소 완료 (보상) - paymentId={}", paymentId);
    }

    /**
     * 결제 상태 업데이트 (Saga 진행 중)
     */
    @Transactional
    public void updatePaymentStatus(Long paymentId, String status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다. ID: " + paymentId));

        // 상태별 처리 (간단하게 로그만)
        log.info("결제 상태 업데이트 - paymentId={}, status={}", paymentId, status);
    }

    /**
     * 결제 실패 처리
     */
    @Transactional
    public void failPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다. ID: " + paymentId));

        payment.failed();
        paymentRepository.save(payment);

        log.warn("결제 실패 처리 - paymentId={}, reason={}", paymentId, reason);
    }

    /**
     * 결제 완료 처리 (Saga 성공)
     */
    @Transactional
    public void completePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다. ID: " + paymentId));

        payment.succeed();
        paymentRepository.save(payment);

        log.info("결제 완료 - paymentId={}", paymentId);
    }
}
