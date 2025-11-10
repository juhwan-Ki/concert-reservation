package com.gomdol.concert.queue.domain.model;

import lombok.Getter;

@Getter
public class QueueToken {

    private final Long id;
    private final String token;
    private final String userId;
    private final Long targetId;
    private QueueStatus status;
    private long position;
    private long ttlSeconds;

    private QueueToken(Long id, String token, String userId, Long targetId, QueueStatus status, long position, long ttlSeconds) {
        this.id = id;
        this.token = token;
        this.userId = userId;
        this.targetId = targetId;
        this.status = status;
        this.position = position;
        this.ttlSeconds = ttlSeconds;
    }

    public static QueueToken create(String token, String userId, Long targetId, QueueStatus status, long position, long ttlSeconds) {
        return new QueueToken(0L, token, userId, targetId, status, position, ttlSeconds);
    }

    public static QueueToken of(Long id, String token, String userId, Long targetId, QueueStatus status, long position, long ttlSeconds) {
        return new QueueToken(id, token, userId, targetId, status, position, ttlSeconds);
    }

    public boolean isExpired() {
        return status == QueueStatus.EXPIRED || ttlSeconds <= 0;
    }

    public void entered(Long ttlSeconds) {
        this.status = QueueStatus.ENTERED;
        this.ttlSeconds = ttlSeconds;
    }

    public void expired() {
        this.status = QueueStatus.EXPIRED;
    }
}
