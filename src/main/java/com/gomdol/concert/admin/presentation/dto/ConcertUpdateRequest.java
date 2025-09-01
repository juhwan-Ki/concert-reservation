package com.gomdol.concert.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "콘서트 수정 요청")
public record ConcertUpdateRequest(
        @Schema(description="공연장 id", example="1")
        Long venueId,

        @Schema(description="콘서트명", example="임영웅 IM HERO TOUR 2025")
        String title,

        @Schema(description="아티스트명", example="임영웅")
        String artist,

        @Schema(description="공연정보", example="IM HERO TOUR 2025 영웅시대를 위한...")
        @Size(max = 5000) String description,

        @Schema(description="공연시간", example="150분")
        String runtime,

        @Schema(description="관람연령", example="만7세이상")
        String ageRating,

        @Schema(description="공연시작일자", example = "2025-08-12")
        String startDate,

        @Schema(description="공연종료일자", example = "2025-08-20")
        String endDate,

        @Schema(description="섬네일 이미지 url", example = "https://imga-asdasdasc.com?asdasdas")
        String thumbnailUrl,

        @Schema(description="포스터 이미지 url", example = "https://imga-asdasdasc.com?asdasdas")
        String posterUrl,

        @Schema(description = "공연목록")
        List<ShowRequest> showRequestList
) {

}

