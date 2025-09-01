package com.gomdol.concert.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 응답 (로그인/재발급 공용)")
public record TokenResponse(
    @Schema(example = "Bearer")
    String tokenType,

    @Schema(description = "JWT Access Token", example = "eyJhbGciOi...")
    String accessToken,

    @Schema(example = "1800", description = "Access 만료(초)")
    Integer expiresIn,

    @Schema(description = "JWT Refresh Token (로그인 시 항상, 재발급 시 선택적)", nullable = true, example = "rft_eyJhbGciOi...")
    String refreshToken,

    @Schema(example = "1209600", description = "Refresh 만료(초, 로그인 시에만 의미)", nullable = true)
    Integer refreshExpiresIn
) {

}
