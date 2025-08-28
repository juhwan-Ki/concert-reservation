package com.gomdol.concert.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest (
    @Schema(description = "사용자 이메일", example = "user@example.com")
    @NotBlank @Email String loginId,

    @Schema(description = "비밀번호", example = "P@ssw0rd!")
    @NotBlank String password
) {

}
