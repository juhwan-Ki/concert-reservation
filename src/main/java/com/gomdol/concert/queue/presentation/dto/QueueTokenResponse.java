package com.gomdol.concert.queue.presentation.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대기열 토큰 응답")
public record QueueTokenResponse(
        @Schema(example = "qtok_abc123xyz")
        String queueToken,

        @Schema(example = "c1a2b3d4-e5f6-7890-1234-56789abcdef0")
        String userId,

        @Schema(example = "1523")
        int position,

        @Schema(example = "420")
        int estimatedWaitSeconds
) {}
