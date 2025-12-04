package com.gomdol.concert.point.application.usecase;

import com.gomdol.concert.common.application.idempotency.port.in.CreateIdempotencyKey;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.point.application.port.in.SavePointPort;
import com.gomdol.concert.point.domain.model.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 충전/사용 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavePointUseCase implements SavePointPort {

    private final CreateIdempotencyKey createIdempotencyKey;
    private final PointRepository pointRepository;
    private final PointHistoryRepository historyRepository;

    @Transactional
    public PointSaveResponse savePoint(PointRequest req) {
        String userId = req.userId();
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
        PointHistory savedHistory = historyRepository.save(PointHistory.create(userId, req.requestId(), amount, type, before, after, java.time.LocalDateTime.now()));

        // 멱등성 키 저장 - 성공적으로 처리된 요청 기록
        createIdempotencyKey.createIdempotencyKey(req.requestId(), userId, ResourceType.POINT, savedHistory.getId());

        log.info("포인트 작업 완료 - userId={} type={} amount={} before={} after={}", userId, type, amount, before, after);
        return PointSaveResponse.fromDomain(point,savedHistory.getId());
    }
}
