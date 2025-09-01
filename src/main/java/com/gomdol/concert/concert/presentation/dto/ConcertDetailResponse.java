package com.gomdol.concert.concert.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ConcertDetailResponse(

        @Schema(example = "만 7세이상", description = "관람연령")
        String ageRating,

        @Schema(example = "150분", description = "공연시간")
        String runtime,

        @Schema(example = "셰익스피어의 대표작을 현대적으로 재해석한...", description = "공연정보")
        String description,

        @Schema(description="포스터 이미지 url", example = "https://imga-asdasdasc.com?asdasdas")
        String posterUrl,

        @Schema(description = "공연 리스트")
        List<ShowResponse> showList
){

}
