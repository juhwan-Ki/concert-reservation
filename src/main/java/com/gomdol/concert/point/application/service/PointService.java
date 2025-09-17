package com.gomdol.concert.point.application.service;

import com.gomdol.concert.point.domain.history.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.domain.point.Point;
import com.gomdol.concert.point.domain.policy.PointPolicy;
import com.gomdol.concert.point.domain.repository.PointHistoryRepository;
import com.gomdol.concert.point.domain.repository.PointRepository;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointHistoryRepository historyRepository;

    // 포인트 조회
    public PointResponse getPoint(String userId) {
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("포인트가 존재하지 않습니다"));
        return PointResponse.fromDomain(point);
    }

    // 포인트 충전/사용
    @Transactional
    public PointResponse savePoint(String userId, PointRequest req) {
        String requestId = req.requestId();
        Optional<PointHistory> existed = historyRepository.findByUserIdAndRequestId(userId, requestId);
        if (existed.isPresent()) {
            PointHistory history = existed.get();
            // 이미 처리됨 → 과거 afterBalance 기반으로 응답 재구성
            return PointResponse.fromDomain(Point.create(history.getUserId(), history.getAfterBalance()));
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
        historyRepository.save(PointHistory.create(userId, req.requestId(), amount, type, before, after));

        return PointResponse.fromDomain(point);
    }

    // 포인트 이력 조회
    // 포인트 이력 저장
}
