package com.gomdol.concert.payment.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 결제 요청 이벤트
 * API 요청 시 발행 → Kafka → PointEventConsumer가 포인트 사용 처리
 */
@Getter
public class PaymentRequestedEvent {
    private final Long paymentId;
    private final Long reservationId;
    private final String userId;
    private final String requestId;
    private final long amount;
    private final LocalDateTime occurredAt;

    private PaymentRequestedEvent(
            Long paymentId,
            Long reservationId,
            String userId,
            String requestId,
            long amount,
            LocalDateTime occurredAt
    ) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.userId = userId;
        this.requestId = requestId;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    public static PaymentRequestedEvent of(
            Long paymentId,
            Long reservationId,
            String userId,
            String requestId,
            long amount
    ) {
        return new PaymentRequestedEvent(
                paymentId,
                reservationId,
                userId,
                requestId,
                amount,
                LocalDateTime.now()
        );
    }
}
