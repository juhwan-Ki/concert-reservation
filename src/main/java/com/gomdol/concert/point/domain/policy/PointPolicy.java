package com.gomdol.concert.point.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PointPolicy {
    public static final long MAX_CHARGE_AMOUNT = 1_000_000L;
    public static final long MIN_USE_UNIT = 1000L;

    public static void validateCharge(long amount) {
        if (amount > MAX_CHARGE_AMOUNT)
            throw new IllegalArgumentException("최대 충전 금액을 초과하였습니다.");
    }

    public static void validateUse(long amount) {
        if (amount % MIN_USE_UNIT != 0)
            throw new IllegalArgumentException("최소 사용 단위보다 작은 단위입니다.");
    }
}
