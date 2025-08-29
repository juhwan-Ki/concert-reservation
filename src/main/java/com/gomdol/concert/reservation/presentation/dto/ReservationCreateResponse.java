package com.gomdol.concert.reservation.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "좌석 예약 응답")
public record ReservationCreateResponse(
        @Schema(description = "예약 ID", example = "5001")
        Long reservationId,

        @Schema(description = "예약 상태", example = "HOLD")
        String status,

        @Schema(description = "결제 마감 시각(만료)", example = "2025-09-01T20:00:00+09:00")
        LocalDateTime expiresAt,

        @Schema(description = "총액", example = "300000")
        Long totalAmount
) {}
