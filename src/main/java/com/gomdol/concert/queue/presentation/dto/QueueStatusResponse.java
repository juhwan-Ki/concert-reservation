package com.gomdol.concert.queue.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대기열 상태 응답")
public record QueueStatusResponse(
        @Schema(example = "1523")
        int position,

        @Schema(example = "400")
        int estimatedWaitSeconds,

        @Schema(example = "false")
        boolean active
) {

}
