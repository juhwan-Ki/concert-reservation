package com.gomdol.concert.point.domain.repository;

import com.gomdol.concert.point.domain.point.Point;

import java.util.Optional;

public interface PointRepository {
    Optional<Point> findByUserId(String userId);
    Point save(Point point);
}
