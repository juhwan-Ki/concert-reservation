package com.gomdol.concert.payment.application.port.out;

import com.gomdol.concert.payment.domain.model.Payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findByRequestId(String requestId);
    Optional<Payment> findById(Long id);
}
