package com.gomdol.concert.point.domain.history;

import com.gomdol.concert.point.domain.model.UseType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PointHistory {
    private final Long id;
    private final Long userId;
    private final long amount;
    private final UseType useType;
    private final long beforeBalance;
    private final long afterBalance;
    private final LocalDateTime createdAt;

    private PointHistory(Long id, Long userId, long amount, UseType useType, long beforeBalance, long afterBalance, LocalDateTime createdAt) {
        validateUser(userId);
        validateAmount(amount);
        validateBalance(beforeBalance, afterBalance);
        this.id = id;
        this.userId = userId;
        this.useType = useType;
        this.amount = changeNegateAmount(amount);
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
        this.createdAt = createdAt;
    }

    public static PointHistory create(Long id, Long userId, long amount, UseType useType, long beforeBalance, long afterBalance, LocalDateTime createdAt) {
        return new PointHistory(id, userId, amount, useType, beforeBalance, afterBalance, createdAt);
    }

    private void validateUser(Long userId) {
        if (userId == null || userId <= 0)
            throw new IllegalArgumentException("ID는 null이거나 0보다 작을 수 없습니다.");
    }

    private void validateAmount(long amount) {
        if(amount <= 0)
            throw new IllegalArgumentException("금액은 양수여야 합니다.");
    }

    private long changeNegateAmount(long amount) {
        return this.useType.equals(UseType.USE) ? -amount : amount;
    }

    private void validateBalance(long beforeBalance, long afterBalance) {
        if(beforeBalance <= 0 || afterBalance <= 0)
            throw new IllegalArgumentException("잔액은 양수여야 합니다.");
    }
}
