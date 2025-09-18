package com.gomdol.concert.point.domain.repository;

import com.gomdol.concert.point.domain.history.PointHistory;

import java.util.Optional;

public interface PointHistoryRepository {
    PointHistory save(PointHistory pointHistory);
    Optional<PointHistory> findByUserIdAndRequestId(String userId, String requestId);
}
