package com.gomdol.concert.point.application.facade;

import com.gomdol.concert.point.application.port.in.SavePointPort;
import com.gomdol.concert.point.application.usecase.SavePointUseCase;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;

/**
 * 포인트 작업 Facade
 * - 비관적 락 타임아웃 발생 시 재시도
 * - 실제 비즈니스 로직은 SavePointUseCase에 위임 (REQUIRES_NEW 트랜잭션)
 * - 추후 분산락 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointFacade implements SavePointPort {

    private final SavePointUseCase savePointUseCase;

    @Override
    public PointResponse savePoint(String userId, PointRequest req) {
        String requestId = req.requestId();
        int maxRetries = 3;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // SavePointUseCase가 REQUIRES_NEW 트랜잭션으로 실행됨
                return savePointUseCase.savePoint(userId, req);
            } catch (PessimisticLockException | LockTimeoutException | CannotAcquireLockException e) {
                if (attempt == maxRetries - 1) {
                    log.error("포인트 저장 실패 - 최대 재시도 횟수 초과 userId={} requestId={} attempt={}",
                            userId, requestId, attempt + 1, e);
                    throw new IllegalStateException("처리 중인 요청이 너무 많습니다. 잠시 후 다시 시도하세요.", e);
                }

                log.warn("포인트 저장 재시도 중 userId={} requestId={} attempt={}/{} exception={}",
                        userId, requestId, attempt + 1, maxRetries, e.getClass().getSimpleName());

                try {
                    Thread.sleep(100 * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("재시도 중 인터럽트 발생", ie);
                }
            }
        }

        throw new IllegalStateException("포인트 저장 실패");
    }
}
