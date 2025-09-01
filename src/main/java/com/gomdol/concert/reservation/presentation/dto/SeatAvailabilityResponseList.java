package com.gomdol.concert.reservation.presentation.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "좌석 목록 조회")
public record SeatAvailabilityResponseList(
        @ArraySchema(arraySchema = @Schema(description = "좌석 목록"))
        List<SeatResponse> seats
) {}