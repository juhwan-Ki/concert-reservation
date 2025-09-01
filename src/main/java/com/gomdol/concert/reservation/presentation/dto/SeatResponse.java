package com.gomdol.concert.reservation.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "좌석 단건 정보")
public record SeatResponse(
        @Schema(description = "좌석 ID", example = "31005")
        Long seatId,

        @Schema(description = "좌석 표기", example = "A-12")
        String seatLabel,

        @Schema(description = "가격", example = "150000")
        BigDecimal price,

        @Schema(description = "예약 가능 여부", example = "true")
        boolean available
) {

}
