package com.gomdol.concert.payment.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "바로 결제 요청")
public record ChargeRequest(
        @Schema(description = "예약 ID", example = "5001")
        @NotNull Long reservationId,

        @Schema(description = "결제 금액", example = "300000")
        @NotNull @Positive Long amount
) {}
