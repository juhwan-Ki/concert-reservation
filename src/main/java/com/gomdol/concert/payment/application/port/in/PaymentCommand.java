package com.gomdol.concert.payment.application.port.in;

public record PaymentCommand(
        Long reservationId,
        String userId,
        String requestId,
        long amount
) {
}
