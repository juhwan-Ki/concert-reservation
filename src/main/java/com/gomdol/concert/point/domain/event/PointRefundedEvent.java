package com.gomdol.concert.point.domain.event;

import java.time.LocalDateTime;

/**
 * 포인트 환불 완료 Event
 * Point Service가 Payment Service로 전송
 */
public record PointRefundedEvent(Long paymentId, Long reservationId, String userId, String requestId, long amount, boolean succeeded, String failureReason, LocalDateTime occurredAt) {

    public static PointRefundedEvent success(Long paymentId, Long reservationId, String userId, String requestId, long amount) {
        return new PointRefundedEvent(paymentId, reservationId, userId, requestId, amount, true, null, LocalDateTime.now());
    }

    public static PointRefundedEvent failure(Long paymentId, Long reservationId, String userId, String requestId, long amount, String failureReason) {
        return new PointRefundedEvent(paymentId, reservationId, userId, requestId, amount, false, failureReason, LocalDateTime.now());
    }
}
