package com.gomdol.concert.reservation.domain.command;

import java.time.LocalDateTime;

/**
 * 좌석 취소 Command (보상 트랜잭션)
 * Payment Service가 Reservation Service로 전송
 *
 * @param reason 취소 사유
 */
public record CancelSeatsCommand(Long paymentId, Long reservationId, String userId, String requestId, String reason, LocalDateTime createdAt) {

    public static CancelSeatsCommand of(Long paymentId, Long reservationId, String userId, String requestId, String reason) {
        return new CancelSeatsCommand(paymentId, reservationId, userId, requestId, reason, LocalDateTime.now());
    }
}
