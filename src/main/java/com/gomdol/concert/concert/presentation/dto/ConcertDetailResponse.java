package com.gomdol.concert.concert.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ConcertDetailResponse(
        @Schema(example = "1", description = "콘서트 id")
        Long id,

        @Schema(example = "보컬 전쟁 시즌2 “The War of Vocalists II”", description = "콘서트 명")
        String title,

        @Schema(example = "인천 인스파이어 아레나", description = "공연장 명")
        String venueName,

        @Schema(example = "QWER", description = "아티스트 명")
        String artist,

        @Schema(example = "만 7세이상", description = "관람연령")
        String ageRating,

        @Schema(example = "150분", description = "공연시간")
        String runningTime,

        @Schema(example = "셰익스피어의 대표작을 현대적으로 재해석한...", description = "공연정보")
        String description,

        @Schema(description="포스터 이미지 url", example = "https://imga-asdasdasc.com?asdasdas")
        String posterUrl,

        @Schema(example = "2025-08-12", description = "시작일")
        LocalDate startAt,

        @Schema(example = "2025-08-12", description = "종료일")
        LocalDate endAt,

        @Schema(description = "공연 리스트")
        List<ShowResponse> showList
){

}
