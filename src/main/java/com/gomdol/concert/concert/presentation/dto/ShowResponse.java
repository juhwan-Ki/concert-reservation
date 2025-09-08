package com.gomdol.concert.concert.presentation.dto;

import com.gomdol.concert.show.domain.Show;
import com.gomdol.concert.show.domain.ShowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Schema(description = "공연 단건")
public record ShowResponse(
        @Schema(example = "1", description = "공연 id")
        Long id,

        @Schema(example = "판매중", description = "공연 상태")
        ShowStatus showStatus,

        @Schema(example = "2025-08-12:12:00", description = "공연 시작 시각")
        LocalDateTime showAt
) {
        public static ShowResponse from(Show show) {
                return new ShowResponse(
                        show.getId(),
                        show.getStatus(),
                        show.getShowAt()
                );
        }
}
