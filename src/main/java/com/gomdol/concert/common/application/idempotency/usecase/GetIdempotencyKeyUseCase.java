package com.gomdol.concert.common.application.idempotency.usecase;

import com.gomdol.concert.common.application.idempotency.port.in.GetIdempotencyKey;
import com.gomdol.concert.common.application.idempotency.port.out.IdempotencyKeyRepository;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 멱등성 키 조회 UseCase
 * - 이미 처리된 요청인지 확인하기 위해 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetIdempotencyKeyUseCase implements GetIdempotencyKey {

    private final IdempotencyKeyRepository idempotencyRepository;

    /**
     * 멱등성 키로 리소스 ID 조회
     * - 이미 처리된 요청인 경우 resourceId 반환
     * - 처음 처리하는 요청인 경우 empty 반환
     *
     * @param requestKey 멱등성 키 (requestId)
     * @param userId 사용자 ID
     * @param type 리소스 타입
     * @return 이미 처리된 경우 resourceId, 아니면 empty
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Long> getIdempotencyKey(String requestKey, String userId, ResourceType type) {
        log.debug("멱등성 키 조회 - key={}, userId={}, type={}", requestKey, userId, type);
        return idempotencyRepository.findByIdempotencyKey(requestKey, userId, type);
    }
}
