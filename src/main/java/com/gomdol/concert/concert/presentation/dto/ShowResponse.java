package com.gomdol.concert.concert.presentation.dto;

import com.gomdol.concert.show.infra.query.projection.ShowProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공연 단건")
public record ShowResponse(
        @Schema(example = "1", description = "공연 id")
        Long id,

        @Schema(example = "판매중", description = "공연 상태")
        String showStatus,

        @Schema(example = "2025-08-12:12:00", description = "공연 시작 시각")
        LocalDateTime showAt
) {
        public static ShowResponse from(ShowProjection show) {
                // 필요하면 label/파생 규칙 적용
                return new ShowResponse(show.getId(), show.getStatus(), show.getShowAt());
        }
}
