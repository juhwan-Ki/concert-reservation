package com.gomdol.concert.user.presentation.dto;

import com.gomdol.concert.user.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SingUpRequest(
        @Schema(description = "사용자 이메일", example = "user@example.com")
        @NotBlank @Email
        String loginId,

        @Schema(description = "비밀번호", example = "P@ssw0rd!")
        @NotBlank
        String password,

        @Schema(description = "사용자 이름", example = "손흥민")
        @NotBlank
        String name,

        @Schema(description = "사용자 권한", example = "USER",
                allowableValues = {"USER", "ADMIN"})
        @NotNull(message = "권한은 필수 값입니다.")
        Role role
) {
}
