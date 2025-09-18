package com.gomdol.concert.point.infra.persistence;

import com.gomdol.concert.point.infra.persistence.entity.PointHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistoryEntity, Long> {
    Optional<PointHistoryEntity> findByUserIdAndRequestId(String userId, String requestId);
}
