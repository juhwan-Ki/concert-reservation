package com.gomdol.concert.queue.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "대기열 진입/상태 확인 요청")
public record EnterQueueRequest(
        @Schema(description = "대기열 토큰", example = "abc123xyz789...")
        @NotBlank(message = "토큰은 필수입니다")
        String token,

//        @Schema(description = "사용자 ID", example = "user123")
//        @NotBlank(message = "사용자 ID는 필수입니다")
//        String userId,

        @Schema(description = "공연 ID", example = "1")
        @NotNull(message = "공연 ID는 필수입니다")
        Long targetId
) {
}
