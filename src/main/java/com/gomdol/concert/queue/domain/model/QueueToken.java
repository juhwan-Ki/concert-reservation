package com.gomdol.concert.queue.domain.model;

import lombok.Getter;

import java.time.Instant;

@Getter
public class QueueToken {
    private final String token;
    private final String userId;
    private final Long targetId;
    private QueueStatus status;
    private Long position;
    private Long totalWaiting;
    private Instant expiresAt;

    private QueueToken(String token, String userId, Long targetId, QueueStatus status, Long position, Long totalWaiting, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.targetId = targetId;
        this.status = status;
        this.position = position;
        this.totalWaiting = totalWaiting;
        this.expiresAt = expiresAt;
    }

    public static QueueToken create(String token, String userId, Long targetId, QueueStatus status, Long position, Long totalWaiting, Instant expiresAt) {
        return new QueueToken(token, userId, targetId, status, position, totalWaiting, expiresAt);
    }
}
