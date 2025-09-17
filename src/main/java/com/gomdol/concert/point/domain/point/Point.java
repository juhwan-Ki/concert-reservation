package com.gomdol.concert.point.domain.point;

import com.gomdol.concert.point.domain.policy.PointPolicy;
import lombok.Getter;

@Getter
public class Point {
    private final String userId;
    private long balance;

    private Point(String userId, Long balance) {
        validateUser(userId);
        if (balance < 0) throw new IllegalArgumentException("초기 잔액은 음수일 수 없습니다");
        this.userId = userId;
        this.balance = balance;
    }

    public static Point create(String userId, Long balance) {
        return new Point(userId, balance);
    }

    private void validateUser(String userId) {
        if (userId == null || userId.length() != 36)
            throw new IllegalArgumentException("요청한 사용자 ID가 올바른 형식이 아닙니다.");
    }

    public void usePoint(long amount) {
        PointPolicy.validateUse(amount);
        if(balance <= 0 || balance < amount)
            throw new IllegalArgumentException("잔액이 부족합니다.");
        this.balance -= amount;
    }

    public void changeBalance(long amount) {
        PointPolicy.validateCharge(amount);
        if(amount <= 0)
            throw new IllegalArgumentException("충전금액은 0보다 커야합니다.");
        this.balance += amount;
    }
}
