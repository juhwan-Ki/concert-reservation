package com.gomdol.concert.point.domain.point;

import com.gomdol.concert.point.domain.policy.PointPolicy;
import lombok.Getter;

@Getter
public class Point {
    private final Long userId;
    private long balance;

    private Point(Long userId, Long balance) {
        validateUser(userId);
        if (balance < 0) throw new IllegalArgumentException("초기 잔액은 음수일 수 없습니다");
        this.userId = userId;
        this.balance = balance;
    }

    public static Point create(Long userId, Long balance) {
        return new Point(userId, balance);
    }

    private void validateUser(Long userId) {
        if (userId == null || userId <= 0)
            throw new IllegalArgumentException("ID는 null이거나 0보다 작을 수 없습니다.");
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
