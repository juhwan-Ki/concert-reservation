package com.gomdol.concert.queue.presentation.dto;


import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대기열 토큰 응답")
public record QueueTokenResponse(
        @Schema(description = "대기열 토큰", example = "qtok_abc123xyz")
        String token,

        @Schema(description = "대기열 상태", example = "WAITING")
        String status,

        @Schema(description = "현재 대기 순서", example = "10")
        Long position,

        @Schema(description = "대기열을 요청한 콘서트 or 공연 ID", example = "100")
        Long targetId,

        @Schema(description = "만료 TTL(초)", example = "50")
        Long ttlSeconds
) {
        public static QueueTokenResponse fromDomain(QueueToken queueToken) {
                return new QueueTokenResponse(queueToken.getToken(), queueToken.getStatus().name(), queueToken.getPosition(), queueToken.getTargetId(), queueToken.getTtlSeconds());
        }

        public boolean isWaiting() {
            return QueueStatus.WAITING.name().equals(status);
        }
}
