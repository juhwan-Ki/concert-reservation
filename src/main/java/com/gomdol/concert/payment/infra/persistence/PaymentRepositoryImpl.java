package com.gomdol.concert.payment.infra.persistence;

import com.gomdol.concert.payment.application.port.out.PaymentRepository;
import com.gomdol.concert.payment.domain.model.Payment;
import com.gomdol.concert.payment.infra.persistence.entitiy.PaymentEntity;
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

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentJpaRepository.findById(id).map(PaymentEntity::toDomain);
    }
}
