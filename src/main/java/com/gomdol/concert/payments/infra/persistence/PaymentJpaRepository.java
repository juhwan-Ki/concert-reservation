package com.gomdol.concert.payments.infra.persistence;

import com.gomdol.concert.payments.infra.persistence.entitiy.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByRequestId(String requestId);
}
