package com.gomdol.concert.point.domain.event;

import java.time.LocalDateTime;

/**
 * 포인트 사용 완료 Event
 * Point Service가 PaymentSagaConsumer로 전송
 *
 * @param succeeded     성공 여부
 * @param failureReason 실패 사유
 */
public record PointUsedEvent(Long paymentId, Long reservationId, String userId, String requestId, long amount, boolean succeeded, String failureReason, LocalDateTime occurredAt) {

    public static PointUsedEvent success(Long paymentId, Long reservationId, String userId, String requestId, long amount) {
        return new PointUsedEvent(paymentId, reservationId, userId, requestId, amount, true, null, LocalDateTime.now());
    }

    public static PointUsedEvent failure(Long paymentId, Long reservationId, String userId, String requestId, long amount, String failureReason) {
        return new PointUsedEvent(paymentId, reservationId, userId, requestId, amount, false, failureReason, LocalDateTime.now());
    }
}
