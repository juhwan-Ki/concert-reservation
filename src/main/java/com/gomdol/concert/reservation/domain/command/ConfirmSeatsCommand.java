package com.gomdol.concert.reservation.domain.command;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 좌석 확정 Command
 * PaymentOrchestrator가 Reservation Service로 전송
 */
@Getter
public class ConfirmSeatsCommand {
    private final Long paymentId;
    private final Long reservationId;
    private final String userId;
    private final String requestId;
    private final LocalDateTime createdAt;

    private ConfirmSeatsCommand(
            Long paymentId,
            Long reservationId,
            String userId,
            String requestId,
            LocalDateTime createdAt
    ) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.userId = userId;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    public static ConfirmSeatsCommand of(
            Long paymentId,
            Long reservationId,
            String userId,
            String requestId
    ) {
        return new ConfirmSeatsCommand(
                paymentId,
                reservationId,
                userId,
                requestId,
                LocalDateTime.now()
        );
    }
}
