package com.gomdol.concert.point.presentation.dto;

import com.gomdol.concert.point.domain.point.Point;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "포인트 응답")
public record PointResponse(
        @Schema(example = "123e4567-e89b-12d3-a456-426614174000", description = "사용자ID")
        String userId,

        @Schema(example = "1800", description = "잔액")
        Long balance
) {
        public static PointResponse fromDomain(Point point) {
                return new PointResponse(point.getUserId(), point.getBalance());
        }
}
