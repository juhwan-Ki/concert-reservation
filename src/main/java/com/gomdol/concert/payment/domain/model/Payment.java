package com.gomdol.concert.payment.domain.model;

import com.gomdol.concert.payment.domain.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.gomdol.concert.user.domain.policy.UserPolicy.*;

@Getter
public class Payment {
    private final Long id;
    private final Long reservationId;
    private final String userId;
    private final String paymentCode;
    private final String requestId;
    private final long amount;
    private PaymentStatus status;
    private LocalDateTime paidAt;

    private Payment(Long id, Long reservationId, String userId, String paymentCode, String requestId, long amount, PaymentStatus status, LocalDateTime paidAt) {
        validateLongValue(reservationId, "reservationId");
        validateUser(userId);
        validateStringValue(paymentCode, "paymentCode");
        validateStringValue(requestId, "requestId");
        validateAmount(amount);
        validatePaidAt(paidAt);

        this.id = id;
        this.reservationId = reservationId;
        this.userId = userId;
        this.paymentCode = paymentCode;
        this.requestId = requestId;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
    }

    public static Payment create(Long reservationId, String userId, String paymentCode, String requestId, long amount) {
        return new Payment(null, reservationId, userId, paymentCode, requestId, amount, PaymentStatus.PENDING, null);
    }

    // DB에서 조회한 데이터
    public static Payment of(Long id, Long reservationId, String userId, String paymentCode, String requestId, long amount, PaymentStatus status, LocalDateTime paidAt) {
      return new Payment(id, reservationId, userId, paymentCode, requestId, amount, status, paidAt);
    }

    // 비즈니스 메서드
    public void succeed() {
        if (this.status != PaymentStatus.PENDING)
            throw new IllegalStateException("PENDING 상태에서만 성공 처리할 수 있습니다");

        this.status = PaymentStatus.SUCCEEDED;
        this.paidAt = LocalDateTime.now();
    }

    public void failed() {
        if (this.status != PaymentStatus.PENDING)
            throw new IllegalStateException("PENDING 상태에서만 실패 처리할 수 있습니다");

        this.status = PaymentStatus.FAILED;
    }

    public boolean isCompleted() {
        return status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED;
    }

    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;

    }

    public boolean isTerminal() { return status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED || status == PaymentStatus.REFUNDED; }

    // 검증 메서드
    public void validateEqualsAmount(long amount) {
        if (this.amount != amount)
            throw new IllegalArgumentException(String.format("결제 금액이 일치하지 않습니다. 예상: %d원, 실제: %d원", amount, this.amount));
    }

    private static void validateAmount(long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
    }

    private void validateStringValue(String value, String field) {
        if(value == null || value.isEmpty())
            throw new IllegalArgumentException(field + "는 반드시 존재해야합니다.");
    }

    private void validateLongValue(Long value, String field) {
        if(value == null || value < 0)
            throw new IllegalArgumentException(field + "0보다 커야합니다.");
    }

    private static void validatePaidAt(LocalDateTime paidAt) {
        if (paidAt != null && paidAt.isAfter(LocalDateTime.now()))
            throw new IllegalStateException("결제 시간은 현재 시간 이전이거나 같아야 합니다.");
    }
}
