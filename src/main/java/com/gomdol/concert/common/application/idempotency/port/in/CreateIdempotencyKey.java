package com.gomdol.concert.common.application.idempotency.port.in;

import com.gomdol.concert.common.domain.idempotency.ResourceType;

public interface CreateIdempotencyKey {
    void createIdempotencyKey(String idempotencyKey, String userId, ResourceType type, Long resourceId);
}
