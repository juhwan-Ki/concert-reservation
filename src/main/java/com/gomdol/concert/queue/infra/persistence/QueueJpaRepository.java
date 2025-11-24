package com.gomdol.concert.queue.infra.persistence;

import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.infra.persistence.entity.QueueTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QueueJpaRepository extends JpaRepository<QueueTokenEntity, Long> {

    Optional<QueueTokenEntity> findByTargetIdAndUserIdAndStatusIn(Long targetId, String token, List<QueueStatus> status);
    Optional<QueueTokenEntity> findByTokenAndTargetId(String token, Long targetId);

    @Query("select count(q) " +
            "from QueueTokenEntity q " +
            "where q.targetId=:targetId " +
            "and q.status='WAITING' " +
            "and q.id < :id " +
            "and q.expiresAt > :now "
    )
    long countWaitingAhead(@Param("targetId") Long targetId, @Param("id") Long id, @Param("now") Instant now);

    /*
     * 현재 대기열이 존재하는 공연장 ID 리턴
     * */
    @Query("""
        select distinct q.targetId
        from QueueTokenEntity q
        where q.status = 'WAITING'
          and q.expiresAt > :now
    """)
    List<Long> findActiveTargetIds(@Param("now") Instant now);

    /*
     * 상태에 따른 대기열 토큰 갯수 리턴
     * */
    @Query("SELECT COUNT(t) FROM QueueTokenEntity t WHERE t.targetId = :targetId and t.status = :status")
    long countByTargetIdAndStatus(@Param("targetId") Long targetId, @Param("status") QueueStatus status);

    /*
     * 현재 대기열의 진입하고 있는 사용자 수 리턴
     * */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
      select count(q) from QueueTokenEntity q
      where q.targetId = :targetId
        and q.status = 'ENTERED'
        and q.expiresAt > :now
    """)
    long countEnteredActive(@Param("targetId") Long targetId, @Param("now") Instant now);

    /*
     * 현재 waiting 상태인 대기열 토큰의 정보 리턴 -> 비관적락 적용
     * */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT * FROM queue_token q " +
            "WHERE q.target_id = :targetId AND q.status = 'WAITING' " +
            "AND q.expires_at > :now " +
            "ORDER BY q.id ASC LIMIT :limit " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<QueueTokenEntity> findAndLockWaitingTokens(@Param("targetId") Long targetId, @Param("now") Instant now, @Param("limit") int limit);

    /*
     * 만료된 대기열 토큰의 정보를 리턴
     * */
    @Query(value = """
      select * from queue_token q
      where q.status IN (:statuses)
        and q.expires_at <= :now
      limit :limit
    """, nativeQuery = true)
    List<QueueTokenEntity> findAllExpiredAndOffsetLimit(@Param("statuses")List<QueueStatus> queueStatus, @Param("now") Instant now, @Param("limit")int limit);
}
