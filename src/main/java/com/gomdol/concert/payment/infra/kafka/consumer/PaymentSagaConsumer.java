package com.gomdol.concert.payment.infra.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gomdol.concert.common.application.outbox.OutboxRepository;
import com.gomdol.concert.common.domain.outbox.OutboxEvent;
import com.gomdol.concert.payment.application.usecase.PaymentSaveUseCase;
import com.gomdol.concert.point.domain.command.RefundPointCommand;
import com.gomdol.concert.point.domain.event.PointUsedEvent;
import com.gomdol.concert.reservation.domain.command.CancelSeatsCommand;
import com.gomdol.concert.reservation.domain.command.ConfirmSeatsCommand;
import com.gomdol.concert.reservation.domain.event.SeatsConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.gomdol.concert.point.domain.command.RefundPointCommand.*;

/**
 * Payment Service용 Saga Consumer
 * 다른 서비스로부터 이벤트를 수신하여 Saga를 진행시킴
 *
 * 수신하는 이벤트:
 * 1. PointUsedEvent (from Point Service)
 * 2. SeatsConfirmedEvent (from Reservation Service)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSagaConsumer {

    private final PaymentSaveUseCase paymentSaveUseCase;

    @Qualifier("paymentOutboxRepositoryImpl")
    private final OutboxRepository paymentOutboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${kafka.topics.confirm-seats-command}")
    private String confirmSeatsCommandTopic;

    @Value("${kafka.topics.cancel-seats-command}")
    private String cancelSeatsCommandTopic;

    @Value("${kafka.topics.refund-point-command}")
    private String refundPointCommandTopic;

    /**
     * PointUsedEvent 수신 처리
     *
     * [성공 시]
     * - Payment 상태: PENDING → PROCESSING
     * - ConfirmSeatsCommand 발행
     *
     * [실패 시 - 보상 트랜잭션]
     * - Payment 상태: PENDING → FAILED
     * - CancelSeatsCommand 발행 (예약 취소)
     */
    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.point-used-event}",
            groupId = "${kafka.consumer.payment-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePointUsedEvent(
            @Payload PointUsedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("=== [Payment Service] PointUsedEvent 수신 - paymentId={}, succeeded={}, offset={} ===", event.paymentId(), event.succeeded(), offset);

        try {
            if (event.succeeded())
                handlePointUsedSuccess(event);
            else
                handlePointUsedFailure(event);

            acknowledgment.acknowledge();
            log.info("[Payment Service] PointUsedEvent 처리 완료");
        } catch (Exception e) {
            log.error("[Payment Service] PointUsedEvent 처리 중 예외 발생", e);
            throw e;  // 재시도
        }
    }

    /**
     * 포인트 사용 성공 → 좌석 확정 진행
     */
    private void handlePointUsedSuccess(PointUsedEvent event) {
        // Payment 상태 업데이트: PENDING → PROCESSING
        paymentSaveUseCase.updatePaymentStatus(event.paymentId(), "PROCESSING");
        log.info("[Payment Service] Payment 상태 업데이트: PROCESSING");

        // ConfirmSeatsCommand를 Outbox에 저장
        ConfirmSeatsCommand command = ConfirmSeatsCommand.of(event.paymentId(), event.reservationId(), event.userId(), event.requestId());
        saveOutboxEvent(event.paymentId().toString(), "ConfirmSeatsCommand", confirmSeatsCommandTopic, command);
        log.info("[Payment Service] ConfirmSeatsCommand Outbox 저장 완료");
    }

    /**
     * 포인트 사용 실패 → 보상 트랜잭션 (예약 취소)
     */
    private void handlePointUsedFailure(PointUsedEvent event) {
        // Payment 상태 업데이트: PENDING → FAILED
        paymentSaveUseCase.failPayment(event.paymentId(), "포인트 사용 실패: " + event.failureReason());
        log.warn("[Payment Service] Payment 실패 처리 - reason={}", event.failureReason());

        // CancelSeatsCommand를 Outbox에 저장 (보상 트랜잭션)
        CancelSeatsCommand command = CancelSeatsCommand.of(event.paymentId(), event.reservationId(), event.userId(), event.requestId(), "포인트 사용 실패");
        saveOutboxEvent(event.paymentId().toString(), "CancelSeatsCommand", cancelSeatsCommandTopic, command);
        log.info("[Payment Service] CancelSeatsCommand Outbox 저장 완료 (보상 트랜잭션)");
    }

    /**
     * SeatsConfirmedEvent 수신 처리
     *
     * [성공 시]
     * - Payment 상태: PROCESSING → COMPLETED
     *
     * [실패 시 - 보상 트랜잭션]
     * - Payment 상태: PROCESSING → FAILED
     * - RefundPointCommand 발행 (포인트 환불)
     */
    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.seats-confirmed-event}",
            groupId = "${kafka.consumer.payment-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSeatsConfirmedEvent(
            @Payload SeatsConfirmedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("=== [Payment Service] SeatsConfirmedEvent 수신 - paymentId={}, succeeded={}, offset={} ===", event.paymentId(), event.succeeded(), offset);

        try {
            if (event.succeeded())
                handleSeatsConfirmedSuccess(event);
            else
                handleSeatsConfirmedFailure(event);

            acknowledgment.acknowledge();
            log.info("[Payment Service] SeatsConfirmedEvent 처리 완료");

        } catch (Exception e) {
            log.error("[Payment Service] SeatsConfirmedEvent 처리 중 예외 발생", e);
            throw e;  // 재시도
        }
    }

    /**
     * 좌석 확정 성공 → Payment 완료
     */
    private void handleSeatsConfirmedSuccess(SeatsConfirmedEvent event) {
        // Payment 상태 업데이트: PROCESSING → COMPLETED
        paymentSaveUseCase.completePayment(event.paymentId());
        log.info("=== [Payment Service] Saga 완료 - paymentId={}, status=COMPLETED ===", event.paymentId());
    }

    /**
     * 좌석 확정 실패 → 보상 트랜잭션 (포인트 환불)
     */
    private void handleSeatsConfirmedFailure(SeatsConfirmedEvent event) {
        // Payment 상태 업데이트: PROCESSING → FAILED
        paymentSaveUseCase.failPayment(event.paymentId(), "좌석 확정 실패: " + event.failureReason());
        log.warn("[Payment Service] Payment 실패 처리 - reason={}", event.failureReason());

        // 2. RefundPointCommand를 Outbox에 저장 (보상 트랜잭션)
        // Note: Payment에서 amount 정보를 조회해야 함 (실제 구현 시)
        // 여기서는 간단히 event에서 가져온다고 가정
        RefundPointCommand command = of(event.paymentId(), event.reservationId(), event.userId(), event.requestId() + "-refund",
                0L,  // TODO: Payment에서 amount 조회 필요
                "좌석 확정 실패"
        );

        saveOutboxEvent(event.paymentId().toString(), "RefundPointCommand", refundPointCommandTopic, command);
        log.info("[Payment Service] RefundPointCommand Outbox 저장 완료 (보상 트랜잭션)");
    }

    private void saveOutboxEvent(String aggregateId, String eventType, String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.create("PAYMENT", aggregateId, eventType, topic, payload);
            paymentOutboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("[Payment Outbox] 이벤트 직렬화 실패", e);
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
