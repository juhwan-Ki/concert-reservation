package com.gomdol.concert.reservation.domain.event;

import java.time.LocalDateTime;

/**
 * 좌석 취소 완료 Event
 * Reservation Service가 Payment로 전송
 */
public record SeatsCancelledEvent(Long paymentId, Long reservationId, String userId, String requestId, boolean succeeded, String failureReason, LocalDateTime occurredAt) {

    public static SeatsCancelledEvent success(Long paymentId, Long reservationId, String userId, String requestId) {
        return new SeatsCancelledEvent(paymentId, reservationId, userId, requestId, true, null, LocalDateTime.now());
    }

    public static SeatsCancelledEvent failure(Long paymentId, Long reservationId, String userId, String requestId, String failureReason) {
        return new SeatsCancelledEvent(paymentId, reservationId, userId, requestId, false, failureReason, LocalDateTime.now());
    }
}
