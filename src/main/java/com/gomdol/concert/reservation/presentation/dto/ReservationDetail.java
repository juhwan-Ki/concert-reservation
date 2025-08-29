package com.gomdol.concert.reservation.presentation.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "예약 상세")
public record ReservationDetail(
        @Schema(description = "예약 ID", example = "5001")
        Long reservationId,

        @Schema(description = "회차 ID", example = "202")
        Long showId,

        @Schema(description = "콘서트 ID", example = "101")
        Long concertId,

        @Schema(description = "예약 상태", example = "RESERVED")
        String status,

        @Schema(description = "콘서트명", example = "히구치 아이 밴드 투어")
        String concertTitle,

        @Schema(description = "공연명", example = "히구치 아이 밴드 투어 2025-2026 in Seoul “wishing me happiness”")
        String showTitle,

        @Schema(description = "공연시간", example = "2025-09-11T20:00:00")
        LocalDateTime showAt,

        @Schema(description = "총액", example = "300000")
        Long totalAmount,

        @ArraySchema(arraySchema = @Schema(description = "예약 좌석 목록"))
        List<ReservedSeat> seats
) {}
