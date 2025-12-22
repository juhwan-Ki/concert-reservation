package com.gomdol.concert.reservation.domain.event;

import java.time.LocalDateTime;

/**
 * 좌석 확정 완료 Event
 * Reservation Service가 Payment로 전송
 */
public record SeatsConfirmedEvent(Long paymentId, Long reservationId, String userId, String requestId, boolean succeeded, String failureReason, LocalDateTime occurredAt) {

    public static SeatsConfirmedEvent success(Long paymentId, Long reservationId, String userId, String requestId) {
        return new SeatsConfirmedEvent(paymentId, reservationId, userId, requestId, true, null, LocalDateTime.now());
    }

    public static SeatsConfirmedEvent failure(Long paymentId, Long reservationId, String userId, String requestId, String failureReason) {
        return new SeatsConfirmedEvent(paymentId, reservationId, userId, requestId, false, failureReason, LocalDateTime.now());
    }
}
