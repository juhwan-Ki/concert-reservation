package com.gomdol.concert.common.infra.idempotency.persistnence.entity;

import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.common.domain.idempotency.model.IdempotencyKey;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idempotency_key",
                        columnNames = {"idempotency_key"})},
        indexes = {
            @Index(name = "ix_idempotency_key_create_at", columnList = "create_at DESC")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 36)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createdAt;

    private IdempotencyKeyEntity(String idempotencyKey, String userId, Long resourceId,  ResourceType resourceType, LocalDateTime createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.createdAt = createdAt;
    }

    public static IdempotencyKeyEntity fromDomain(IdempotencyKey key) {
        return new IdempotencyKeyEntity(key.getIdempotencyKey(), key.getUserId(), key.getResourceId(), key.getResourceType(), key.getCreatedAt());
    }
}
