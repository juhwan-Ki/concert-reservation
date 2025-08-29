package com.gomdol.concert.concert.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ConcertResponse(
        @Schema(example = "1", description = "콘서트 id")
        Long id,

        @Schema(example = "보컬 전쟁 시즌2 “The War of Vocalists II”", description = "콘서트 명")
        String title,

        @Schema(example = "인천 인스파이어 아레나", description = "공연장 명")
        String venueName,

        @Schema(example = "QWER", description = "아티스트 명")
        String artist,

        @Schema(example = "공개", description = "상태")
        String status,

        @Schema(example = "2025-08-12", description = "시작일")
        LocalDate startAt,

        @Schema(example = "2025-08-12", description = "종료일")
        LocalDate endAt,

        @Schema(description="섬네일 이미지 url", example = "https://imga-asdasdasc.com?asdasdas")
        @NotBlank String thumbnailUrl
) {
}
