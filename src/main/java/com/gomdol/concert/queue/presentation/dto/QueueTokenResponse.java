package com.gomdol.concert.queue.presentation.dto;


import com.gomdol.concert.queue.domain.model.QueueToken;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대기열 토큰 응답")
public record QueueTokenResponse(
        @Schema(description = "대기열 토큰", example = "qtok_abc123xyz")
        String queueToken,

        @Schema(description = "유저 ID", example = "c1a2b3d4-e5f6-7890-1234-56789abcdef0")
        String userId,

        @Schema(description = "대기열 상태", example = "c1a2b3d4-e5f6-7890-1234-56789abcdef0")
        String status,

        @Schema(description = "대기열을 요청한 콘서트 or 공연 ID", example = "100")
        Long targetId,

        @Schema(description = "현재 대기 순번", example = "30")
        Long position,

        @Schema(description = "전체 대기열 크기", example = "50")
        Long totalWaiting
) {
        public static QueueTokenResponse fromDomain(QueueToken queueToken) {
                return new QueueTokenResponse(queueToken.getToken(), queueToken.getUserId(), queueToken.getStatus().name(), queueToken.getTargetId(), queueToken.getPosition(), queueToken.getTotalWaiting());
        }
}
