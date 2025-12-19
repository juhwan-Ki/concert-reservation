package com.gomdol.concert.reservation.infra.persistence.outbox;

import com.gomdol.concert.common.domain.outbox.OutboxEvent;
import com.gomdol.concert.common.domain.outbox.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Reservation Service용 Outbox 엔티티
 */
@Entity
@Table(name = "reservation_outbox",
        indexes = {
                @Index(name = "idx_reservation_outbox_status", columnList = "status"),
                @Index(name = "idx_reservation_outbox_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String aggregateType;  // RESERVATION

    @Column(nullable = false, length = 100)
    private String aggregateId;    // reservationId

    @Column(nullable = false, length = 100)
    private String eventType;      // SeatsConfirmedEvent, SeatsCancelledEvent

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(length = 500)
    private String errorMessage;

    public static ReservationOutboxEntity fromDomain(OutboxEvent event) {
        ReservationOutboxEntity entity = new ReservationOutboxEntity();
        entity.id = event.getId();
        entity.aggregateType = event.getAggregateType();
        entity.aggregateId = event.getAggregateId();
        entity.eventType = event.getEventType();
        entity.topic = event.getTopic();
        entity.payload = event.getPayload();
        entity.status = event.getStatus();
        entity.createdAt = event.getCreatedAt();
        entity.publishedAt = event.getPublishedAt();
        entity.retryCount = event.getRetryCount();
        entity.errorMessage = event.getErrorMessage();
        return entity;
    }

    public OutboxEvent toDomain() {
        return OutboxEvent.of(
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
}
