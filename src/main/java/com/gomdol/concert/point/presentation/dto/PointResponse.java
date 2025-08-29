package com.gomdol.concert.point.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "포인트 응답")
public record PointResponse(
        @Schema(example = "1800", description = "잔액")
        Long balance,

//        @Schema(description = "사용 타입", example = "CHARGE",
//                allowableValues = {"CHARGE", "USE", "REFUND"})
//        UseType useType,

        @Schema(example = "2025-08-29T15:03:00+09:00", description = "잔액 기준 시각(ISO-8601)")
        OffsetDateTime asOf
) {
}
