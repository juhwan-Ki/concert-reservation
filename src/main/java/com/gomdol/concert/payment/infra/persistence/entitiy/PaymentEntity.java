package com.gomdol.concert.payment.infra.persistence.entitiy;

import com.gomdol.concert.common.domain.BaseEntity;
import com.gomdol.concert.payment.domain.PaymentStatus;
import com.gomdol.concert.payment.domain.model.Payment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "payments")
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class PaymentEntity extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservaion_id", nullable = false)
    private Long reservationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "payment_code", nullable = false, unique = true)
    private String paymentCode;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Version
    private long version;

    public static PaymentEntity fromDomain(Payment payment) {
        return PaymentEntity.builder()
                .id(payment.getId())
                .reservationId(payment.getReservationId())
                .userId(payment.getUserId())
                .paymentCode(payment.getPaymentCode())
                .requestId(payment.getRequestId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }

    public static Payment toDomain(PaymentEntity entity) {
        return Payment.of(
                entity.getId(),
                entity.reservationId,
                entity.userId,
                entity.paymentCode,
                entity.requestId,
                entity.amount,
                entity.status,
                entity.paidAt);
    }
}
