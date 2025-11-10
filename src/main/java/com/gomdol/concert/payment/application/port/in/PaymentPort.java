package com.gomdol.concert.payment.application.port.in;

import com.gomdol.concert.payment.presentation.dto.PaymentResponse;

public interface PaymentPort {
    PaymentResponse processPayment(PaymentCommand command);
    record PaymentCommand(Long reservationId, String userId, String requestId, long amount) {}
}
