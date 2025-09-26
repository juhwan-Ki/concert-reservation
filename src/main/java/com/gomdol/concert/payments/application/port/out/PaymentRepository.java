package com.gomdol.concert.payments.application.port.out;

import com.gomdol.concert.payments.domain.model.Payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findByRequestId(String requestId);
}
