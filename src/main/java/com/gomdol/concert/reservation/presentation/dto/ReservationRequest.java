package com.gomdol.concert.reservation.presentation.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "좌석 예약 생성 요청")
public record ReservationRequest(
        @Schema(description = "회차 ID", example = "202")
        @NotNull Long showId,

        @ArraySchema(schema = @Schema(example = "31005"))
        @NotEmpty List<@NotNull Long> seatIds
) {}
