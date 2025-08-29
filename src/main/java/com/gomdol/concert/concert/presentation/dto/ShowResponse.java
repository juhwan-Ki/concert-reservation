package com.gomdol.concert.concert.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;


public record ShowResponse(
        @Schema(example = "1", description = "공연 id")
        Long id,

        @Schema(example = "보컬 전쟁 시즌2 “The War of Vocalists II” - 대전", description = "공연명")
        String showTitle,

        @Schema(example = "인천 인스파이어 아레나", description = "공연장 명")
        String venueName,

        @Schema(example = "2025-08-12:12:00", description = "공연 시작 시각")
        LocalDateTime showAt
) {
}
