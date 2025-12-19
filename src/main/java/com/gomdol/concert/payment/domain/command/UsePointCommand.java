package com.gomdol.concert.payment.domain.command;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 포인트 사용 Command
 * Payment가 Point Service로 전송
 */
@Getter
public class UsePointCommand {
    private final Long paymentId;
    private final Long reservationId;
    private final String userId;
    private final String requestId;
    private final long amount;
    private final LocalDateTime createdAt;

    private UsePointCommand(
            Long paymentId,
            Long reservationId,
            String userId,
            String requestId,
            long amount,
            LocalDateTime createdAt
    ) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.userId = userId;
        this.requestId = requestId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public static UsePointCommand of(
            Long paymentId,
            Long reservationId,
            String userId,
            String requestId,
            long amount
    ) {
        return new UsePointCommand(
                paymentId,
                reservationId,
                userId,
                requestId,
                amount,
                LocalDateTime.now()
        );
    }
}
