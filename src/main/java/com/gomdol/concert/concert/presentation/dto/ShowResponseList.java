package com.gomdol.concert.concert.presentation.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공연 목록")
public record ShowResponseList(
        @ArraySchema(arraySchema = @Schema(description = "공연 목록"))
        List<ShowResponse> items
) {

}
