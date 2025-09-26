package com.gomdol.concert.payments.infra.persistence;

import com.gomdol.concert.payments.application.port.out.PaymentRepository;
import com.gomdol.concert.payments.domain.model.Payment;
import com.gomdol.concert.payments.infra.persistence.entitiy.PaymentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = paymentJpaRepository.save(PaymentEntity.fromDomain(payment));
        return PaymentEntity.toDomain(entity);
    }

    @Override
    public Optional<Payment> findByRequestId(String requestId) {
        return paymentJpaRepository.findByRequestId(requestId).map(PaymentEntity::toDomain);
    }
}
