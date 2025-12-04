package com.gomdol.concert.point.application.port.in;

import com.gomdol.concert.point.domain.model.PointHistory;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GetPointHistoryPort {
    // TODO: 포인트 이력은 추후 개발
    // 포인트 이력 조회
    Optional<PointHistoryResponse> getPointHistory(Long historyId);
    record PointHistoryResponse(Long id, String userId, String type, long amount, long beforeBalance, long afterBalance, LocalDateTime createdAt) {
        public static PointHistoryResponse fromDomain(PointHistory pointHistory) {
            return new PointHistoryResponse(
                    pointHistory.getId(),
                    pointHistory.getUserId(),
                    pointHistory.getUseType().name(),
                    pointHistory.getAmount(),
                    pointHistory.getBeforeBalance(),
                    pointHistory.getAfterBalance(),
                    pointHistory.getCreatedAt()
            );
        }
    }
}
