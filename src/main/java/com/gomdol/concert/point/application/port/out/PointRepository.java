package com.gomdol.concert.point.application.port.out;

import com.gomdol.concert.point.domain.model.Point;

import java.util.Optional;

public interface PointRepository {
    Optional<Point> findByUserId(String userId);
    Point save(Point point);
}
