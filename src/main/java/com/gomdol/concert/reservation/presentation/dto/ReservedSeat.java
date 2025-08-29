package com.gomdol.concert.reservation.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예약된 좌석")
public record ReservedSeat(
        @Schema(description = "좌석 ID", example = "31005")
        Long seatId,

        @Schema(description = "좌석 표기", example = "A-12")
        String seatLabel,

        @Schema(description = "가격", example = "150000")
        Long price
) {}
