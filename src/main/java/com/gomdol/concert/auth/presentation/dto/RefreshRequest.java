package com.gomdol.concert.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Access 재발급 요청")
public record RefreshRequest(
    @Schema(example = "rft_eyJhbGciOi...")
    @NotBlank
    String refreshToken
) {

}
