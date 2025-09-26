package com.gomdol.concert.payments.application.port.in;

public record PaymentCommand(
        Long reservationId,
        String userId,
        String requestId,
        long amount
) {
}
