package com.gomdol.concert.payments.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 요청")
public record PaymentRequest(
        @Schema(description = "예약 ID", example = "5001")
        @NotNull Long reservationId,

        @Schema(description = "금액", example = "50000")
        long amount
) {}
