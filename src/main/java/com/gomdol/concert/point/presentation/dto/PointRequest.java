package com.gomdol.concert.point.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import com.gomdol.concert.point.domain.UseType;

@Schema(description = "포인트 충전/사용 요청")
public record PointRequest (
        @Schema(example = "2000", description = "충천/사용 금액")
        @NotNull Long amount,

        @Schema(description = "사용 타입", example = "CHARGE",
                allowableValues = {"CHARGE", "USE", "REFUND"})
        @NotNull(message = "타입은 필수 값입니다.")
        UseType useType
){

}
