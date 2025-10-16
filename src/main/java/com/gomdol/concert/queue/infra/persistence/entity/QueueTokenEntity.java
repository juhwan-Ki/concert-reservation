package com.gomdol.concert.queue.infra.persistence.entity;

import com.gomdol.concert.common.domain.CreateEntity;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "token", nullable=false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static QueueTokenEntity createWaiting(Long targetId, String userId, String tokenHint, long waitingTtlSeconds) {
        return QueueTokenEntity.builder()
                .targetId(targetId)
                .userId(userId)
                .token(tokenHint)
                .status(QueueStatus.WAITING)
                .expiresAt(Instant.now().plusSeconds(waitingTtlSeconds))
                .build();

    }

    public static QueueToken toDomain(QueueTokenEntity entity, Long position, Long totalWaiting) {
        return QueueToken.create(entity.getToken(), entity.getUserId(), entity.getTargetId(), entity.getStatus(), position, totalWaiting, entity.getExpiresAt());
    }

    public void changeStatus(QueueStatus newStatus) {
        this.status = newStatus;
    }
}
