package com.gomdol.concert.point.application.port.in;

import com.gomdol.concert.point.domain.model.Point;

public interface GetPointBalancePort {
    PointSearchResponse getPoint(String userId);
    record PointSearchResponse(String userId, long balance) {
        public static PointSearchResponse fromDomain(Point point) {
            return new PointSearchResponse(point.getUserId(), point.getBalance());
        }
    }
}
