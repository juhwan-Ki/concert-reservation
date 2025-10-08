package com.gomdol.concert.payment.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "환불 응답")
public record RefundResponse(
        @Schema(description = "환불 처리 상태", example = "REQUESTED")
        String status,

        @Schema(description = "환불 금액", example = "300000")
        Long amount,

        @Schema(description = "환불 처리 일시", example = "2025-09-01T21:00:00")
        LocalDateTime processedAt
) {

}
