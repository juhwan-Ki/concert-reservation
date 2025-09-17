package com.gomdol.concert.point.infra.persistence;

import com.gomdol.concert.point.domain.history.PointHistory;
import com.gomdol.concert.point.domain.repository.PointHistoryRepository;
import com.gomdol.concert.point.infra.persistence.entity.PointHistoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryJpaRepository jpaRepository;

    @Override
    public PointHistory save(PointHistory pointHistory) {
        return PointHistoryEntity.toDomain(jpaRepository.save(PointHistoryEntity.fromDomain(pointHistory)));
    }

    @Override
    public Optional<PointHistory> findByUserIdAndRequestId(String userId, String requestId) {
        return jpaRepository.findByUserIdAndRequestId(userId, requestId).map(PointHistoryEntity::toDomain);
    }
}
