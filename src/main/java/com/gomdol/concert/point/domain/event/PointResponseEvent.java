package com.gomdol.concert.point.domain.event;

public record PointResponseEvent(
        String userId,
        String requestId,
        String message,
        long amount,
        boolean isSucceeded
) {
    public static PointResponseEvent succeededEvent(String userId, String requestId, long amount) {
        return new PointResponseEvent(userId, requestId, null, amount, true);
    }

    public static PointResponseEvent failedEvent(String userId, String requestId, String message, long amount) {
        return new PointResponseEvent(userId, requestId, message, amount,false);
    }
}
