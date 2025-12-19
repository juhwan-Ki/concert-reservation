package com.gomdol.concert.point.infra.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gomdol.concert.common.application.outbox.OutboxRepository;
import com.gomdol.concert.common.domain.outbox.OutboxEvent;
import com.gomdol.concert.payment.domain.command.UsePointCommand;
import com.gomdol.concert.point.application.service.PointCommandService;
import com.gomdol.concert.point.domain.command.RefundPointCommand;
import com.gomdol.concert.point.domain.event.PointRefundedEvent;
import com.gomdol.concert.point.domain.event.PointUsedEvent;
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

/**
 * Point Service용 Saga Consumer
 *
 * 수신하는 Command:
 * 1. UsePointCommand (from Payment Service) - 포인트 사용
 * 2. RefundPointCommand (from Payment Service) - 포인트 환불 (보상 트랜잭션)
 *
 * 발행하는 Event:
 * 1. PointUsedEvent - 포인트 사용 결과
 * 2. PointRefundedEvent - 포인트 환불 결과
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointSagaConsumer {

    private final PointCommandService pointCommandService;

    @Qualifier("pointOutboxRepository")
    private final OutboxRepository pointOutboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${kafka.topics.point-used-event}")
    private String pointUsedEventTopic;

    @Value("${kafka.topics.point-refunded-event}")
    private String pointRefundedEventTopic;

    /**
     * UsePointCommand 수신 처리
     *
     * 하나의 트랜잭션에서:
     * 1. 포인트 사용 처리
     * 2. PointUsedEvent를 Point Outbox에 저장
     */
    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.use-point-command}",
            groupId = "${kafka.consumer.point-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUsePointCommand(
            @Payload UsePointCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("=== [Point Service] UsePointCommand 수신 - paymentId={}, userId={}, amount={}, offset={} ===", command.getPaymentId(), command.getUserId(), command.getAmount(), offset);

        try {
            // 포인트 사용 처리
            pointCommandService.usePoint(command.getUserId(), command.getRequestId(), command.getAmount());
            log.info("[Point Service] 포인트 사용 완료 - userId={}, amount={}", command.getUserId(), command.getAmount());

            // 성공 이벤트를 Point Outbox에 저장
            PointUsedEvent successEvent = PointUsedEvent.success(command.getPaymentId(), command.getReservationId(), command.getUserId(), command.getRequestId(), command.getAmount());
            saveOutboxEvent(command.getPaymentId().toString(), "PointUsedEvent", pointUsedEventTopic, successEvent);
            acknowledgment.acknowledge();
            log.info("[Point Service] PointUsedEvent(success) Outbox 저장 완료");

        } catch (IllegalArgumentException e) {
            // 포인트 사용 실패 (잔액 부족 등)
            log.warn("[Point Service] 포인트 사용 실패 - userId={}, reason={}", command.getUserId(), e.getMessage());

            // 실패 이벤트를 Point Outbox에 저장
            PointUsedEvent failureEvent = PointUsedEvent.failure(command.getPaymentId(), command.getReservationId(), command.getUserId(), command.getRequestId(), command.getAmount(), e.getMessage());
            saveOutboxEvent(command.getPaymentId().toString(), "PointUsedEvent", pointUsedEventTopic, failureEvent);
            acknowledgment.acknowledge();
            log.info("[Point Service] PointUsedEvent(failure) Outbox 저장 완료");
        } catch (Exception e) {
            log.error("[Point Service] UsePointCommand 처리 중 예외 발생", e);
            throw e;  // 재시도
        }
    }

    /**
     * RefundPointCommand 수신 처리 (보상 트랜잭션)
     *
     * 하나의 트랜잭션에서:
     * 1. 포인트 환불 처리
     * 2. PointRefundedEvent를 Point Outbox에 저장
     */
    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.refund-point-command}",
            groupId = "${kafka.consumer.point-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeRefundPointCommand(
            @Payload RefundPointCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("=== [Point Service] RefundPointCommand 수신 (보상 트랜잭션) - paymentId={}, userId={}, amount={}, offset={} ===", command.getPaymentId(), command.getUserId(), command.getAmount(), offset);

        try {
            // 포인트 환불 처리
            pointCommandService.refundPoint(command.getUserId(), command.getRequestId(), command.getAmount(), command.getReason());
            log.info("[Point Service] 포인트 환불 완료 - userId={}, amount={}, reason={}", command.getUserId(), command.getAmount(), command.getReason());

            // 성공 이벤트를 Point Outbox에 저장
            PointRefundedEvent successEvent = PointRefundedEvent.success(command.getPaymentId(), command.getReservationId(), command.getUserId(), command.getRequestId(), command.getAmount());
            saveOutboxEvent(command.getPaymentId().toString(),"PointRefundedEvent", pointRefundedEventTopic, successEvent);
            acknowledgment.acknowledge();
            log.info("[Point Service] PointRefundedEvent(success) Outbox 저장 완료");
        } catch (Exception e) {
            log.error("[Point Service] RefundPointCommand 처리 중 예외 발생 - 환불 실패는 심각한 문제!", e);
            // 환불 실패 이벤트 발행 (수동 처리 필요)
            PointRefundedEvent failureEvent = PointRefundedEvent.failure(command.getPaymentId(), command.getReservationId(), command.getUserId(), command.getRequestId(), command.getAmount(), e.getMessage());
            saveOutboxEvent(command.getPaymentId().toString(), "PointRefundedEvent", pointRefundedEventTopic, failureEvent);
            acknowledgment.acknowledge();
            log.error("[Point Service] PointRefundedEvent(failure) Outbox 저장 완료 - 수동 처리 필요!");
        }
    }

    private void saveOutboxEvent(String aggregateId, String eventType, String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.create("POINT", aggregateId, eventType, topic, payload);
            pointOutboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("[Point Outbox] 이벤트 직렬화 실패", e);
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
