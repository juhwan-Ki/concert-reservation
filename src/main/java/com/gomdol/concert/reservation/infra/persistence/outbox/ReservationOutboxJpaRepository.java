package com.gomdol.concert.reservation.infra.persistence.outbox;

import com.gomdol.concert.common.domain.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationOutboxJpaRepository extends JpaRepository<ReservationOutboxEntity, Long> {

    List<ReservationOutboxEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Query("SELECT e FROM ReservationOutboxEntity e " +
           "WHERE e.status = 'FAILED' " +
           "AND e.retryCount < :maxRetries " +
           "ORDER BY e.createdAt ASC")
    List<ReservationOutboxEntity> findFailedEventsForRetry(@Param("maxRetries") int maxRetries);

    @Query("SELECT e FROM ReservationOutboxEntity e " +
           "WHERE e.status = 'PUBLISHED' " +
           "AND e.publishedAt < :threshold")
    List<ReservationOutboxEntity> findOldPublishedEvents(@Param("threshold") LocalDateTime threshold);
}
