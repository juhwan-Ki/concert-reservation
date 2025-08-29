package com.gomdol.concert.point.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record PointHistoryItem(
        @Schema(example = "USE", description = "포인트 사용 타입")
        String type,

        @Schema(example = "1800", description = "사용/충전 금액")
        long amount,

        @Schema(example = "10000", description = "사용/충전 후 잔액")
        long balanceAfter,

        @Schema(example = "2025-08-29T15:03:00+09:00", description = "생성 시각(ISO-8601)")
        OffsetDateTime createdAt
) {
}
