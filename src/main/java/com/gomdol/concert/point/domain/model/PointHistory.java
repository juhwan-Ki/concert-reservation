package com.gomdol.concert.point.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

import static com.gomdol.concert.point.domain.policy.PointPolicy.validateAmount;
import static com.gomdol.concert.user.domain.policy.UserPolicy.validateUser;

@Getter
public class PointHistory {
    private Long id;
    private final String userId;
    private final String requestId;
    private final long amount;
    private final UseType useType;
    private final long beforeBalance;
    private final long afterBalance;
    private final LocalDateTime createdAt;

    // DB에서 로딩 시 사용하는 생성자 (amount는 이미 변환된 값)
    public PointHistory(Long id, String userId, String  requestId, long amount, UseType useType, long beforeBalance, long afterBalance, LocalDateTime createdAt) {
        this.createdAt = createdAt;
        validateUser(userId);
        validateRequestId(requestId);
        // DB에서 로딩 시에는 amount가 이미 음수일 수 있음
        validateBalance(beforeBalance, afterBalance);
        // validateAfterBalance는 절대값으로 검증
        validateAfterBalance(afterBalance, beforeBalance, Math.abs(amount), useType);

        this.id = id;
        this.userId = userId;
        this.requestId = requestId;
        this.useType = useType;
        this.amount = amount;  // DB에서 로딩 시에는 이미 변환된 값이므로 그대로 사용
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
    }

    private PointHistory(String userId, String requestId, long amount, UseType useType, long beforeBalance, long afterBalance, LocalDateTime createdAt) {
        validateUser(userId);
//        validateRequestId(requestId);
        validateAmount(amount);
        validateBalance(beforeBalance, afterBalance);
        validateAfterBalance(afterBalance, beforeBalance, amount, useType);

        this.userId = userId;
        this.requestId = requestId;
        this.useType = useType;
        this.amount = changeNegateAmount(amount);
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
        this.createdAt = createdAt;
    }

    public static PointHistory create(String userId, String requestId, long amount, UseType useType, long beforeBalance, long afterBalance, LocalDateTime createdAt) {
        return new PointHistory(userId, requestId, amount, useType, beforeBalance, afterBalance, createdAt);
    }

    private long changeNegateAmount(long amount) {
        return this.useType.equals(UseType.USE) ? -amount : amount;
    }

    private void validateBalance(long beforeBalance, long afterBalance) {
        if(beforeBalance < 0 || afterBalance < 0)
            throw new IllegalArgumentException("잔액은 양수여야 합니다.");
    }

    private void validateRequestId(String requestId) {
        if(requestId == null || requestId.isBlank() || requestId.length() != 36)
            throw new IllegalArgumentException("requestId가 올바른 형식이 아닙니다.");
    }

    private void validateAfterBalance(long afterBalance, long beforeBalance, long amount, UseType useType) {
        if(useType.equals(UseType.USE))
        {
            if(afterBalance != (beforeBalance - amount))
                throw new IllegalStateException("잔액 계산 불일치");
        }
        else if(useType.equals(UseType.CHARGE))
        {
            if(afterBalance != (beforeBalance + amount))
                throw new IllegalStateException("잔액 계산 불일치");
        }
    }
}
