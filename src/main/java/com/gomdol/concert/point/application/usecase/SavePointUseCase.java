package com.gomdol.concert.point.application.usecase;

import com.gomdol.concert.point.application.port.in.SavePointPort;
import com.gomdol.concert.point.domain.model.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavePointUseCase implements SavePointPort {

    private final PointRepository pointRepository;
    private final PointHistoryRepository historyRepository;

    // 포인트 충전/사용
    @Transactional
    public PointResponse savePoint(String userId, PointRequest req) {
        String requestId = req.requestId();
        Optional<PointHistory> existed = historyRepository.findByUserIdAndRequestId(userId, requestId);
        if (existed.isPresent()) {
            PointHistory history = existed.get();
            // 이미 처리됨 → 과거 afterBalance 기반으로 응답 재구성
            return new PointResponse(history.getUserId(), history.getAfterBalance());
        }

        UseType type = req.useType();
        long amount = req.amount();
        // 포인트가 없으면 0으로 생성
        Point point = pointRepository.findByUserId(userId)
                .orElseGet(() -> Point.create(userId, 0L));

        long before = point.getBalance();
        switch (type) {
            case CHARGE -> point.changeBalance(amount);
            case USE    -> point.usePoint(amount);
//            case REFUND -> TODO: 환불은 아직 구현 안함
            default     -> throw new IllegalArgumentException("지원하지 않는 유형: " + type);
        }
        long after = point.getBalance();

        // 포인트 저장 → @Version 으로 충돌 감지
        // TODO: 현재는 동시에 들어온 요청 중 하나는 반드시 성공, 다른 건 무조건 실패
        pointRepository.save(point);
        try {
            historyRepository.save(PointHistory.create(userId, req.requestId(), amount, type, before, after));
            return PointResponse.fromDomain(point);
        } catch (DataIntegrityViolationException dup) {
            // 동시에 들어온 '다른 트랜잭션'이 먼저 저장을 끝낸 케이스 → 방금 커밋된 히스토리를 재조회해 같은 응답 반환 (멱등 보장)
            PointHistory currentHistory = historyRepository.findByUserIdAndRequestId(userId, req.requestId()).orElseThrow(() -> dup); // 정말 없다면 그대로 예외 전파
            return PointResponse.fromDomain(Point.create(userId, currentHistory.getAfterBalance()));
        }
    }
}
