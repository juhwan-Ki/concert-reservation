package com.gomdol.concert.point.application.usecase;

import com.gomdol.concert.point.application.port.in.SavePointPort;
import com.gomdol.concert.point.domain.model.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavePointUseCase implements SavePointPort {

    private final PointRepository pointRepository;
    private final PointHistoryRepository historyRepository;

    // 포인트 충전/사용 (재시도 로직 포함)
    @Transactional
    public PointResponse savePoint(String userId, PointRequest req) {
        String requestId = req.requestId();

        // 멱등성 체크
        Optional<PointHistory> existed = historyRepository.findByUserIdAndRequestId(userId, requestId);
        if (existed.isPresent()) {
            PointHistory history = existed.get();
            log.info("포인트 작업 이미 완료됨 - userId={} requestId={} balance={}", userId, requestId, history.getAfterBalance());
            return new PointResponse(history.getUserId(), history.getAfterBalance());
        }

        // 재시도 로직 (비관적 락 타임아웃 대응)
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return executeSavePoint(userId, req);
            } catch (PessimisticLockException | LockTimeoutException | CannotAcquireLockException e) {
                if (attempt == maxRetries - 1) {
                    log.error("포인트 저장 실패 - 최대 재시도 횟수 초과 userId={} requestId={} attempt={}", userId, requestId, attempt + 1, e);
                    throw new IllegalStateException("처리 중인 요청이 너무 많습니다. 잠시 후 다시 시도하세요.", e);
                }
                log.warn("포인트 저장 재시도 중 userId={} requestId={} attempt={}/{} exception={}", userId, requestId, attempt + 1, maxRetries, e.getClass().getSimpleName());

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

    private PointResponse executeSavePoint(String userId, PointRequest req) {
        UseType type = req.useType();
        long amount = req.amount();

        // 포인트가 없으면 0으로 생성 (비관적 락 적용)
        Point point = pointRepository.findByUserIdWithLock(userId).orElseGet(() -> Point.create(userId, 0L));

        long before = point.getBalance();
        switch (type) {
            case CHARGE -> point.changeBalance(amount);
            case USE    -> point.usePoint(amount);
//            case REFUND -> TODO: 환불은 아직 구현 안함
            default     -> throw new IllegalArgumentException("지원하지 않는 유형: " + type);
        }
        long after = point.getBalance();

        // 포인트 저장 (비관적 락으로 Lost Update 방지)
        pointRepository.save(point);

        try {
            historyRepository.save(PointHistory.create(userId, req.requestId(), amount, type, before, after));
            log.info("포인트 작업 완료 - userId={} type={} amount={} before={} after={}", userId, type, amount, before, after);
            return PointResponse.fromDomain(point);
        } catch (DataIntegrityViolationException dup) {
            // 동시에 들어온 '다른 트랜잭션'이 먼저 저장을 끝낸 케이스 → 방금 커밋된 히스토리를 재조회해 같은 응답 반환 (멱등 보장)
            log.warn("히스토리 중복 저장 감지 - userId={} requestId={}", userId, req.requestId());
            PointHistory currentHistory = historyRepository.findByUserIdAndRequestId(userId, req.requestId()).orElseThrow(() -> dup);
            return PointResponse.fromDomain(Point.create(userId, currentHistory.getAfterBalance()));
        }
    }
}
