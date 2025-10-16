package com.gomdol.concert.queue.infra.persistence;

import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.infra.persistence.entity.QueueTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QueueJpaRepository extends JpaRepository<QueueTokenEntity, Long> {
    Optional<QueueTokenEntity> findByTargetIdAndUserIdAndStatusIn(Long targetId, String token, List<QueueStatus> status);

    @Query("select count(q) " +
            "from QueueTokenEntity q " +
            "where q.targetId=:targetId " +
            "and q.status='WAITING' " +
            "and q.id < :id " +
            "and q.expiresAt > :now "
    )
    long countWaitingAhead(@Param("targetId") Long targetId, @Param("id") Long id, @Param("now") Instant now);

    @Query("select count(q) " +
            "from QueueTokenEntity q " +
            "where q.targetId=:targetId " +
            "and q.status='WAITING' " +
            "and q.expiresAt > :now"
    )
    long countWaiting(@Param("targetId") Long targetId);
}
