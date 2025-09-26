package com.gomdol.concert.point.domain.event;

import com.gomdol.concert.point.domain.model.UseType;

public record PointRequestedEvent(
        String userId,
        String requestId,
        UseType useType,
        long amount
) {
    public static PointRequestedEvent useRequest(String userId, String requestId, long amount) {
        return new PointRequestedEvent(userId, requestId, UseType.USE, amount);
    }

    public static PointRequestedEvent chargeRequest(String userId, String requestId, long amount) {
        return new PointRequestedEvent(userId, requestId, UseType.CHARGE, amount);
    }
}
