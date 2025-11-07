package com.gomdol.concert.point.infra.persistence;

import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.infra.persistence.entity.PointEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository {

    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<Point> findByUserId(String userId) {
       return pointJpaRepository.findById(userId).map(PointEntity::toDomain);
    }

    @Override
    public Point save(Point point) {
        // 기존 엔티티가 있으면 업데이트, 없으면 새로 생성
        PointEntity entity = pointJpaRepository.findById(point.getUserId())
                .map(existing -> {
                    // 기존 엔티티 업데이트
                    existing.updateBalance(point.getBalance());
                    return existing;
                })
                .orElse(PointEntity.fromDomain(point));

        PointEntity saved = pointJpaRepository.save(entity);
        return PointEntity.toDomain(saved);
    }
}
