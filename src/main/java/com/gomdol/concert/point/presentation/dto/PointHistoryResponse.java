package com.gomdol.concert.point.presentation.dto;

import com.gomdol.concert.point.domain.model.PointHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        @Schema
        String userId,

        @Schema(example = "USE", description = "포인트 사용 타입")
        String type,

        @Schema(example = "1800", description = "사용/충전 금액")
        long amount,

        @Schema(example = "1800", description = "사용/충전 금액")
        long beforeBalance,

        @Schema(example = "10000", description = "사용/충전 후 잔액")
        long afterBalance,

        @Schema(example = "2025-08-29T15:03:00", description = "생성 시각")
        LocalDateTime createdAt
) {
    public static PointHistoryResponse fromDomain(PointHistory pointHistory) {
        return new PointHistoryResponse(
                pointHistory.getUserId(),
                pointHistory.getUseType().name(),
                pointHistory.getAmount(),
                pointHistory.getBeforeBalance(),
                pointHistory.getAfterBalance(),
                pointHistory.getCreatedAt()
        );
    }
}
