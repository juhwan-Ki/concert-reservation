package com.gomdol.concert.point.application.service;

import com.gomdol.concert.common.application.idempotency.port.in.CreateIdempotencyKey;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.domain.model.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Point 도메인 Command 서비스
 * Orchestrator에서 호출하는 개별 작업 단위
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointCommandService {

    private final CreateIdempotencyKey createIdempotencyKey;
    private final PointRepository pointRepository;
    private final PointHistoryRepository historyRepository;

    /**
     * 포인트 사용
     */
    @Transactional
    public void usePoint(String userId, String requestId, long amount) {
        Point point = pointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new IllegalStateException("포인트 정보를 찾을 수 없습니다: " + userId));

        long before = point.getBalance();
        point.usePoint(amount);
        long after = point.getBalance();
        pointRepository.save(point);
        // 이력 저장
        PointHistory history = historyRepository.save(PointHistory.create(userId, requestId, amount, UseType.USE, before, after, LocalDateTime.now()));

        // 멱등성 키 저장
        createIdempotencyKey.createIdempotencyKey(requestId, userId, ResourceType.POINT, history.getId());
        log.info("포인트 사용 완료 - userId={}, before={}, after={}", userId, before, after);
    }

    /**
     * 포인트 환불 (보상 트랜잭션)
     */
    @Transactional
    public void refundPoint(String userId, String compensationRequestId, long amount, String reason) {
        Point point = pointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new IllegalStateException("포인트 정보를 찾을 수 없습니다: " + userId));

        long before = point.getBalance();
        point.changeBalance(amount);  // 환불 (잔액 증가)
        long after = point.getBalance();
        pointRepository.save(point);

        // 환불 이력 저장
        PointHistory refundHistory = historyRepository.save(PointHistory.create(userId, compensationRequestId, amount, UseType.REFUND, before, after, LocalDateTime.now()));
        // 멱등성 키 저장
        createIdempotencyKey.createIdempotencyKey(compensationRequestId, userId, ResourceType.POINT, refundHistory.getId());
        log.info("포인트 환불 완료 (보상) - userId={}, before={}, after={}, reason={}", userId, before, after, reason);
    }
}
