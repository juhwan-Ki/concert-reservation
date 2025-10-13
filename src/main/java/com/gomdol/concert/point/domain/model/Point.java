package com.gomdol.concert.point.domain.model;

import lombok.Getter;

import static com.gomdol.concert.point.domain.policy.PointPolicy.*;
import static com.gomdol.concert.point.domain.policy.PointPolicy.validateAmount;
import static com.gomdol.concert.user.domain.policy.UserPolicy.validateUser;

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

    public void usePoint(long amount) {
        validateAmount(amount);
        validateUse(amount);
        if(this.balance < amount)
            throw new IllegalArgumentException("잔액이 부족합니다.");
        this.balance -= amount;
    }

    public void changeBalance(long amount) {
        validateAmount(amount);
        validateCharge(amount);
        this.balance += amount;
    }
}
