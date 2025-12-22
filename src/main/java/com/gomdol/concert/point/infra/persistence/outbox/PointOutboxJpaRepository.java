package com.gomdol.concert.point.infra.persistence.outbox;

import com.gomdol.concert.common.domain.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointOutboxJpaRepository extends JpaRepository<PointOutboxEntity, Long> {

    List<PointOutboxEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Query("SELECT e FROM PointOutboxEntity e " +
           "WHERE e.status = 'FAILED' " +
           "AND e.retryCount < :maxRetries " +
           "ORDER BY e.createdAt ASC")
    List<PointOutboxEntity> findFailedEventsForRetry(@Param("maxRetries") int maxRetries);

    @Query("SELECT e FROM PointOutboxEntity e " +
           "WHERE e.status = 'PUBLISHED' " +
           "AND e.publishedAt < :threshold")
    List<PointOutboxEntity> findOldPublishedEvents(@Param("threshold") LocalDateTime threshold);
}
