package com.gomdol.concert.payments.presentation.dto;

import com.gomdol.concert.payments.domain.model.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "결제 응답")
public record PaymentResponse(
        @Schema(description = "결제 ID", example = "pay_abc123")
        Long paymentId,

        @Schema(description = "예약 ID", example = "5001")
        Long reservationId,

        @Schema(description = "결제 상태", example = "SUCCEEDED")
        String status,

        @Schema(description = "결제 금액", example = "300000")
        Long amount,

        @Schema(description = "결제 완료 시간", example = "2025-09-01T20:01:30")
        LocalDateTime paidAt
) {
        public static PaymentResponse fromDomain(Payment payment) {
                return new PaymentResponse(
                        payment.getId(),
                        payment.getReservationId(),
                        payment.getStatus().toString(),
                        payment.getAmount(),
                        payment.getPaidAt()
                );
        }
}
