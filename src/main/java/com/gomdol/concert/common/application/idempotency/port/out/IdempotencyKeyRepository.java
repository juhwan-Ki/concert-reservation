package com.gomdol.concert.common.application.idempotency.port.out;

import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.common.domain.idempotency.model.IdempotencyKey;

import java.util.Optional;

public interface IdempotencyKeyRepository {
    /**
     * 멱등성 키로 리소스 ID 조회
     *
     * @param key 멱등성 키 (requestId)
     * @param userId 사용자 ID
     * @param type 리소스 타입
     * @return 이미 처리된 경우 resourceId, 아니면 empty
     */
    Optional<Long> findByIdempotencyKey(String key, String userId, ResourceType type);

    /**
     * 멱등성 키 저장
     *
     * @param key 멱등성 키 도메인 객체
     */
    void save(IdempotencyKey key);
}
