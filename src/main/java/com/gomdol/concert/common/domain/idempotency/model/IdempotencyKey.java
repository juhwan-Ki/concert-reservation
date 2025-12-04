package com.gomdol.concert.common.domain.idempotency.model;

import com.gomdol.concert.common.domain.idempotency.ResourceType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class IdempotencyKey {

    private final String idempotencyKey;
    private final String userId;
    private final Long resourceId;
    private final ResourceType resourceType;
    private final LocalDateTime createdAt;

    private IdempotencyKey(String idempotencyKey, String userId, Long resourceId, ResourceType resourceType) {
        validateIdempotencyKey(idempotencyKey);
        validateResourceId(resourceId);

        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.createdAt = LocalDateTime.now();
    }

    public static IdempotencyKey create(String requestKey, String userId, ResourceType resourceType, Long resourceId) {
        return new IdempotencyKey(requestKey, userId, resourceId, resourceType);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if(idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("idempotencyKey가 null이거나 비어있습니다.");

        if(idempotencyKey.length() != 36)
            throw new IllegalArgumentException("idempotencyKey는 36자(UUID 형식)여야 합니다.");
    }

    private void validateResourceId(Long resourceId) {
        if(resourceId == null)
            throw new IllegalArgumentException("resourceId가 null입니다.");
    }
}
