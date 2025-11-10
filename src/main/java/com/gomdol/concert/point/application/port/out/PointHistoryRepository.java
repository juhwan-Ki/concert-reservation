package com.gomdol.concert.point.application.port.out;

import com.gomdol.concert.point.domain.model.PointHistory;

import java.util.Optional;

public interface PointHistoryRepository {
    PointHistory save(PointHistory pointHistory);
    Optional<PointHistory> findByUserIdAndRequestId(String userId, String requestId);
}
