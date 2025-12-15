package com.gomdol.concert.common.infra.util;

import java.time.Duration;

public final class CacheUtils {

    public static final Duration PAYMENT_CACHE_TTL = Duration.ofMinutes(10);
    public static final Duration RESERVATION_CACHE_TTL = Duration.ofMinutes(10);
    public static final Duration POINT_CACHE_TTL = Duration.ofMinutes(10);

    public static String paymentResult(String requestId) {
        return "payment:result:" + requestId;
    }

    public static String reservationResult(String requestId) {
        return "reservation:result:" + requestId;
    }

    public static String pointResult(String requestId) {
        return "point:result:" + requestId;
    }
}
