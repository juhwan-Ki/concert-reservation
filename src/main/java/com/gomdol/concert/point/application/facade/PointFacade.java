package com.gomdol.concert.point.application.facade;

import com.gomdol.concert.common.application.cache.port.out.CacheRepository;
import com.gomdol.concert.common.application.idempotency.port.in.GetIdempotencyKey;
import com.gomdol.concert.common.application.lock.port.out.DistributedLock;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.common.infra.config.DistributedLockProperties;
import com.gomdol.concert.point.application.port.in.GetPointHistoryPort;
import com.gomdol.concert.point.application.port.in.GetPointHistoryPort.PointHistoryResponse;
import com.gomdol.concert.point.application.port.in.SavePointPort.PointSaveResponse;
import com.gomdol.concert.point.application.usecase.SavePointUseCase;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.gomdol.concert.common.infra.config.DistributedLockProperties.*;
import static com.gomdol.concert.common.infra.util.CacheUtils.*;

/**
 * 포인트 작업 Facade
 * - Redis 캐시로 빠른 멱등성 체크
 * - DB 멱등키로 영속적 멱등성 보장
 * - Redis 분산 락으로 동시성 제어
 * - 단일 트랜잭션으로 비즈니스 로직 실행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointFacade {

    private final CacheRepository cacheRepository;
    private final GetIdempotencyKey getIdempotencyKey;
    private final DistributedLock distributedLock;
    private final DistributedLockProperties lockProperties;
    private final SavePointUseCase savePointUseCase;
    private final GetPointHistoryPort getPointHistoryPort;

    /**
     * 포인트 충전/사용 with 멱등성 보장 및 분산 락
     * 1. Redis 캐시 체크
     * 2. DB 멱등성 체크 (트랜잭션 전)
     * 3. 분산 락 획득
     * 4. UseCase 호출 (트랜잭션 시작)
     * 5. DB 제약조건 위반 시 멱등성 재확인
     */
    public PointSaveResponse savePoint(PointRequest req) {
        String cacheKey = pointResult(req.requestId());

        // Redis 캐시 체크 (가장 빠른 조회)
        Optional<PointSaveResponse> cached = cacheRepository.get(cacheKey, PointSaveResponse.class);
        if (cached.isPresent()) {
            log.info("Redis 캐시 히트 - requestId={}", req.requestId());
            return cached.get();
        }

        // DB 멱등성 체크 (트랜잭션 전) - 이미 처리된 요청이면 즉시 반환
        PointSaveResponse response = findByRequestId(req, cacheKey);
        if (response != null)
            return response;

        // 분산 락 획득 및 UseCase 호출
        String lockKey = generateLockKey(req.userId());
        LockConfig lockConfig = lockProperties.point();

        return distributedLock.executeWithLock(lockKey, lockConfig.waitTime().toMillis(), lockConfig.leaseTime().toMillis(), TimeUnit.MILLISECONDS,
                () -> executePointSave(req, cacheKey)
        );
    }

    /**
     * 포인트 처리
     * - 성공 시 Redis 캐시에 저장
     * - DB 제약조건 위반 시 멱등성 키로 재확인
     * - 이미 처리된 요청이면 기존 이력 반환 및 캐시 저장
     */
    private PointSaveResponse executePointSave(PointRequest req, String cacheKey) {
        try {
            log.info("포인트 처리 시작 - userId={}, requestId={}, type={}, amount={}", req.userId(), req.requestId(), req.useType(), req.amount());
            PointSaveResponse response = savePointUseCase.savePoint(req);
            // 성공 시 캐시에 저장
            cacheRepository.set(cacheKey, response, POINT_CACHE_TTL);
            log.info("포인트 캐시 저장 - requestId={}, balance={}", req.requestId(), response.balance());
            return response;
        } catch (DataIntegrityViolationException e) {
            log.warn("제약조건 위반 발생 - userId={} requestId={} error={}", req.userId(), req.requestId(), e.getMessage());
            // 멱등성 키로 다시 조회
            PointSaveResponse response = findByRequestId(req, cacheKey);
            if (response != null)
                return response;
            // 멱등성 키가 없으면 다른 제약조건 위반
            throw new IllegalStateException("포인트 처리 중 제약조건 위반", e);
        }
    }

    /**
     * 멱등키 조회
     * - 멱등키가 등록되어 있으면 동일한 값 반환
     */
    private PointSaveResponse findByRequestId(PointRequest req, String cacheKey) {
        Optional<Long> existingId = getIdempotencyKey.getIdempotencyKey(
                req.requestId(),
                req.userId(),
                ResourceType.POINT
        );

        if (existingId.isPresent()) {
            log.info("멱등성 보장: 기존 포인트 이력 반환 - requestId={}, historyId={}", req.requestId(), existingId.get());
            PointHistoryResponse history = getPointHistoryPort.getPointHistory(existingId.get())
                    .orElseThrow(() -> new IllegalStateException("멱등성 키는 존재하나 포인트 이력을 찾을 수 없습니다."));
            PointSaveResponse response = new PointSaveResponse(history.id(), history.userId(), history.afterBalance());
            // 캐시에 저장 (다음 요청을 위해)
            cacheRepository.set(cacheKey, response, POINT_CACHE_TTL);
            return response;
        }
        return null;
    }

    /**
     * 락 키 생성: point:user:{userId}
     * - 같은 사용자의 포인트 작업은 순차적으로 처리
     */
    private String generateLockKey(String userId) {
        return String.format("point:user:%s", userId);
    }
}
