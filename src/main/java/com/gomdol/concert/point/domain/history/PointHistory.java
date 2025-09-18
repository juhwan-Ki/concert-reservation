package com.gomdol.concert.point.domain.history;

import com.gomdol.concert.point.domain.model.UseType;
import lombok.Getter;

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

    public PointHistory(Long id, String userId, String  requestId, long amount, UseType useType, long beforeBalance, long afterBalance) {
        validateUser(userId);
        validateRequestId(requestId);
        validateAmount(amount);
        validateBalance(beforeBalance, afterBalance);
        validateAfterBalance(afterBalance, beforeBalance, amount, useType);

        this.id = id;
        this.userId = userId;
        this.requestId = requestId;
        this.useType = useType;
        this.amount = changeNegateAmount(amount);
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
    }

    private PointHistory(String userId, String requestId, long amount, UseType useType, long beforeBalance, long afterBalance) {
        validateUser(userId);
        validateRequestId(requestId);
        validateAmount(amount);
        validateBalance(beforeBalance, afterBalance);
        validateAfterBalance(afterBalance, beforeBalance, amount, useType);

        this.userId = userId;
        this.requestId = requestId;
        this.useType = useType;
        this.amount = changeNegateAmount(amount);
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
    }

    public static PointHistory create(String userId, String requestId, long amount, UseType useType, long beforeBalance, long afterBalance) {
        return new PointHistory(userId, requestId, amount, useType, beforeBalance, afterBalance);
    }

    private long changeNegateAmount(long amount) {
        return this.useType.equals(UseType.USE) ? -amount : amount;
    }

    private void validateBalance(long beforeBalance, long afterBalance) {
        if(beforeBalance < 0 || afterBalance < 0)
            throw new IllegalArgumentException("잔액은 양수여야 합니다.");
    }

    private void validateRequestId(String requestId) {
        if(requestId == null || requestId.isBlank() || requestId.length() != 64)
            throw new IllegalArgumentException("requestId가 올바른 형식이 아닙니다.");
    }

    private void validateAfterBalance(long afterBalance, long beforeBalance, long amount, UseType useType) {
        if(useType.equals(UseType.USE))
        {
            if(afterBalance != beforeBalance - amount)
                throw new IllegalStateException("잔액 계산 불일치");
        }
        else if(useType.equals(UseType.CHARGE))
        {
            if(afterBalance != beforeBalance + amount)
                throw new IllegalStateException("잔액 계산 불일치");
        }
    }
}
