package com.gomdol.concert.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "reservation")
public record ReservationProperties(
        @Min(1) @Max(30) int holdMinutes,
        @Min(1) @Max(10) int maxSeatsPerReservation,
        @Min(1) @Max(10) int maxRetryCount
) {}
