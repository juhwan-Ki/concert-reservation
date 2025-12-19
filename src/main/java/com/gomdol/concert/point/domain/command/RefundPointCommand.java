package com.gomdol.concert.point.domain.command;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 포인트 환불 Command (보상 트랜잭션)
 * Payment Service가 Point Service로 전송
 */
@Getter
public class RefundPointCommand {
    private final Long paymentId;
    private final Long reservationId;
    private final String userId;
    private final String requestId;
    private final long amount;
    private final String reason;  // 환불 사유
    private final LocalDateTime createdAt;

    private RefundPointCommand(
            Long paymentId,
            Long reservationId,
            String userId,
            String requestId,
            long amount,
            String reason,
            LocalDateTime createdAt
    ) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.userId = userId;
        this.requestId = requestId;
        this.amount = amount;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public static RefundPointCommand of(
            Long paymentId,
            Long reservationId,
            String userId,
            String requestId,
            long amount,
            String reason
    ) {
        return new RefundPointCommand(
                paymentId,
                reservationId,
                userId,
                requestId,
                amount,
                reason,
                LocalDateTime.now()
        );
    }
}
