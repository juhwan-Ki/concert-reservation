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
        PointEntity entity = pointJpaRepository.save(PointEntity.fromDomain(point));
        return PointEntity.toDomain(entity);
    }
}
