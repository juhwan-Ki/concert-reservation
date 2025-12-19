package com.gomdol.concert.common.domain.outbox;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Outbox 이벤트 도메인 모델
 * Kafka로 발행해야 할 이벤트를 DB에 저장
 */
@Getter
public class OutboxEvent {
    private final Long id;
    private final String aggregateType;  // PAYMENT, RESERVATION 등
    private final String aggregateId;    // 집합 루트 ID
    private final String eventType;      // PaymentCompletedEvent, ReservationCompletedEvent 등
    private final String topic;          // Kafka 토픽
    private final String payload;        // JSON 직렬화된 이벤트
    private final OutboxStatus status;   // PENDING, PUBLISHED, FAILED
    private final LocalDateTime createdAt;
    private final LocalDateTime publishedAt;
    private final int retryCount;
    private final String errorMessage;

    private OutboxEvent(
            Long id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String topic,
            String payload,
            OutboxStatus status,
            LocalDateTime createdAt,
            LocalDateTime publishedAt,
            int retryCount,
            String errorMessage
    ) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
    }

    /**
     * 새로운 Outbox 이벤트 생성
     */
    public static OutboxEvent create(
            String aggregateType,
            String aggregateId,
            String eventType,
            String topic,
            String payload
    ) {
        return new OutboxEvent(
                null,
                aggregateType,
                aggregateId,
                eventType,
                topic,
                payload,
                OutboxStatus.PENDING,
                LocalDateTime.now(),
                null,
                0,
                null
        );
    }

    /**
     * DB에서 조회한 기존 이벤트
     */
    public static OutboxEvent of(
            Long id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String topic,
            String payload,
            OutboxStatus status,
            LocalDateTime createdAt,
            LocalDateTime publishedAt,
            int retryCount,
            String errorMessage
    ) {
        return new OutboxEvent(
                id,
                aggregateType,
                aggregateId,
                eventType,
                topic,
                payload,
                status,
                createdAt,
                publishedAt,
                retryCount,
                errorMessage
        );
    }

    /**
     * 발행 성공 처리
     */
    public OutboxEvent markAsPublished() {
        return new OutboxEvent(
                this.id,
                this.aggregateType,
                this.aggregateId,
                this.eventType,
                this.topic,
                this.payload,
                OutboxStatus.PUBLISHED,
                this.createdAt,
                LocalDateTime.now(),
                this.retryCount,
                null
        );
    }

    /**
     * 발행 실패 처리
     */
    public OutboxEvent markAsFailed(String errorMessage) {
        return new OutboxEvent(
                this.id,
                this.aggregateType,
                this.aggregateId,
                this.eventType,
                this.topic,
                this.payload,
                OutboxStatus.FAILED,
                this.createdAt,
                this.publishedAt,
                this.retryCount + 1,
                errorMessage
        );
    }
}
