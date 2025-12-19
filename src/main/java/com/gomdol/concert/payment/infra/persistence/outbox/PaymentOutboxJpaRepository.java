package com.gomdol.concert.payment.infra.persistence.outbox;

import com.gomdol.concert.common.domain.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutboxEntity, Long> {

    List<PaymentOutboxEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Query("SELECT e FROM PaymentOutboxEntity e " +
           "WHERE e.status = 'FAILED' " +
           "AND e.retryCount < :maxRetries " +
           "ORDER BY e.createdAt ASC")
    List<PaymentOutboxEntity> findFailedEventsForRetry(@Param("maxRetries") int maxRetries);

    @Query("SELECT e FROM PaymentOutboxEntity e " +
           "WHERE e.status = 'PUBLISHED' " +
           "AND e.publishedAt < :threshold")
    List<PaymentOutboxEntity> findOldPublishedEvents(@Param("threshold") LocalDateTime threshold);
}
