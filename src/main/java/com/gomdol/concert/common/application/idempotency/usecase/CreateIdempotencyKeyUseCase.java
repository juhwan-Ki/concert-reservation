package com.gomdol.concert.common.application.idempotency.usecase;

import com.gomdol.concert.common.application.idempotency.port.in.CreateIdempotencyKey;
import com.gomdol.concert.common.application.idempotency.port.out.IdempotencyKeyRepository;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.common.domain.idempotency.model.IdempotencyKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 멱등성 키 생성 UseCase
 * - 처리 완료된 요청의 requestId를 저장하여 중복 처리 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateIdempotencyKeyUseCase implements CreateIdempotencyKey {

    private final IdempotencyKeyRepository idempotencyRepository;

    /**
     * 멱등성 키 생성 및 저장
     * - 독립적인 트랜잭션으로 실행 (REQUIRES_NEW)
     * - 메인 로직이 실패해도 멱등성 키는 저장되지 않도록
     *
     * @param idempotencyKey 멱등성 키 (requestId)
     * @param userId 사용자 ID
     * @param type 리소스 타입
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIdempotencyKey(String idempotencyKey, String userId, ResourceType type, Long resourceId) {
        log.debug("멱등성 키 저장 - key={}, userId={}, type={}, resourceId={}", idempotencyKey, userId, type, resourceId);

        IdempotencyKey key = IdempotencyKey.create(idempotencyKey, userId, type, resourceId);
        idempotencyRepository.save(key);

        log.info("멱등성 키 저장 완료 - key={}, resourceId={}", idempotencyKey, resourceId);
    }
}