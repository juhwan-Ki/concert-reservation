package com.gomdol.concert.queue.application.port.out;

import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QueueRepository {
    // 발급
    QueueToken issueToken(Long targetId, String userId, String token, QueueStatus status, long waitingTtlSeconds);
    QueueToken findQueuePositionByTargetIdAndUserId(Long targetId, String userId);
    boolean isWaiting(Long targetId);

    // 조회
    Optional<QueueToken> findByTargetIdAndUserId(Long targetId, String userId);
    Optional<QueueToken> findByTargetIdAndToken(Long targetId, String userId);
    List<Long> findActiveTargetIds(Instant now);

    // 스케줄러
    long countEnteredActive(Long targetId, Instant now);
    void save(QueueToken token);
    List<QueueToken> findAllTargetIdAndStatusAndOffsetLimit(Long targetId, QueueStatus queueStatus, Instant now, int limit);
    List<QueueToken> findAllExpiredAndOffsetLimit(Instant now, int limit);
}
