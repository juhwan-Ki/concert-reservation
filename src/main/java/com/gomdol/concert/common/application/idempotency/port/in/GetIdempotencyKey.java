package com.gomdol.concert.common.application.idempotency.port.in;

import com.gomdol.concert.common.domain.idempotency.ResourceType;

import java.util.Optional;

public interface GetIdempotencyKey {
    /**
     * 멱등성 키로 이미 처리된 리소스 ID 조회
     *
     * @param requestKey 멱등성 키 (requestId)
     * @param userId 사용자 ID
     * @param type 리소스 타입
     * @return 이미 처리된 경우 resourceId, 아니면 empty
     */
    Optional<Long> getIdempotencyKey(String requestKey, String userId, ResourceType type);
}
