package com.gomdol.concert.common.application.idempotency.service;

import com.gomdol.concert.common.application.cache.port.out.CacheRepository;
import com.gomdol.concert.common.application.idempotency.port.in.GetIdempotencyKey;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private final GetIdempotencyKey getIdempotencyKey;
    private final CacheRepository cacheRepository;

    /**
     * 멱등키 기반 리소스 조회
     *
     * @param requestId 요청 ID
     * @param userId 사용자 ID
     * @param resourceType 리소스 타입
     * @param cacheKey 캐시 키
     * @param cacheTtl 캐시 TTL
     * @param entityFinder 엔티티 조회 함수
     * @param responseMapper 응답 변환 함수
     * @return 조회된 응답 또는 null
     */
    public <T, R> R findByIdempotencyKey(String requestId, String userId, ResourceType resourceType, String cacheKey,
                                         Duration cacheTtl, Function<Long, Optional<T>> entityFinder, Function<T, R> responseMapper) {
        // 멱등키 조회
        Optional<Long> existingId = getIdempotencyKey.getIdempotencyKey(requestId, userId, resourceType);
        if (existingId.isEmpty())
            return null;

        // 엔티티 조회
        T entity = entityFinder.apply(existingId.get())
                .orElseThrow(() -> new IllegalStateException(String.format("멱등성 키는 존재하나 %s를 찾을 수 없습니다.", resourceType)));
        log.info("멱등키 발견: 기존 리소스 반환 - requestId={}, resourceId={}, type={}", requestId, existingId.get(), resourceType);

        // 응답 변환 및 캐싱
        R response = responseMapper.apply(entity);
        cacheRepository.set(cacheKey, response, cacheTtl);

        return response;
    }
}
