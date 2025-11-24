package com.gomdol.concert.queue.infra.persistence.entity;

import com.gomdol.concert.common.infra.persistence.entity.CreateEntity;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name="queue_token",
        uniqueConstraints = {
                @UniqueConstraint(name="uq_target_user", columnNames={"target_id", "user_id"}),
                @UniqueConstraint(name="uq_token", columnNames={"token"})
        },
        indexes = {
                @Index(name = "ix_queue_wait", columnList = "target_id, status, expires_at, id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class QueueTokenEntity extends CreateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "user_id", nullable=false)
    private String userId;

    @Column(name = "token")
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static QueueTokenEntity create(Long targetId, String userId, String token, QueueStatus status, long waitingTtlSeconds) {
        return QueueTokenEntity.builder()
                .targetId(targetId)
                .userId(userId)
                .token(token)
                .status(status)
                .expiresAt(Instant.now().plusSeconds(waitingTtlSeconds))
                .build();
    }

    public static QueueTokenEntity fromDomain(QueueToken queueToken) {
        return QueueTokenEntity.builder()
                .id(queueToken.getId())
                .targetId(queueToken.getTargetId())
                .userId(queueToken.getUserId())
                .token(queueToken.getToken())
                .status(queueToken.getStatus())
                .expiresAt(Instant.now().plusSeconds(queueToken.getTtlSeconds()))
                .build();
    }

    public static QueueToken toDomain(QueueTokenEntity entity) {
        long ttl = Math.max(0, Duration.between(Instant.now(), entity.expiresAt).toSeconds());
        return QueueToken.of(entity.getId(), entity.getToken(), entity.getUserId(), entity.getTargetId(), entity.getStatus(), 0, ttl);
    }

    public static QueueToken toDomainWithPosition(QueueTokenEntity entity, Long position) {
        long ttl = Math.max(0, Duration.between(Instant.now(), entity.expiresAt).toSeconds());
        return QueueToken.of(entity.getId(), entity.getToken(), entity.getUserId(), entity.getTargetId(), entity.getStatus(), position, ttl);
    }

    public void changeStatus(QueueStatus newStatus) {
        this.status = newStatus;
    }
}
