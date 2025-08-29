package com.gomdol.concert.admin.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// TODO: 현재는 콘서트와 공연이 1:1로 매칭되게 함 -> 추후 1:N으로 매핑해서 여러개의 공연을 처리하도록 변경 필요
@Schema(description = "공연 생성 요청")
public record ShowRequest(
        @Schema(description = "공연장 id", example = "1")
        @NotNull Long venueId,

        @Schema(description = "공연제목", example = "임영웅 IM HERO TOUR 2025 - 인천")
        @NotBlank String title,

        @Schema(description = "공연일자", example = "2025-08-12:13:00")
        @NotBlank String showDate
) {
}
