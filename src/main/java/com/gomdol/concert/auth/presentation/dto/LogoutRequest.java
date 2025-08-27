package com.gomdol.concert.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그아웃 요청")
public record LogoutRequest(
    @Schema(example = "rft_eyJhbGciOi...")
    String refreshToken
) {
}
