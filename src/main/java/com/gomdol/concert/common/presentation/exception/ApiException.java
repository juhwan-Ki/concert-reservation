package com.gomdol.concert.common.presentation.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 에러 응답")
public record ApiException(
    @Schema(example = "TOKEN_EXPIRED")
    String code,

    @Schema(example = "토큰이 만료되었습니다.")
    String message
) {

}
