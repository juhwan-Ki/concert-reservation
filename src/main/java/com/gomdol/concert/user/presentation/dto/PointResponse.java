package com.gomdol.concert.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record PointResponse(
        @Schema(example = "1800", description = "잔액")
        long balance,

        @Schema(example = "2025-08-28T16:53:23.438", description = "생성일자")
        LocalDateTime createAt,

        @Schema(example = "2025-08-28T16:53:23.438", description = "수정일자")
        LocalDateTime updateAt
) {
}
