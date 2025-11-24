package com.gomdol.concert.common.infra.config;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "queue")
public record QueueProperties(
        @Min(8) @Max(64) int tokenLength,
        @Min(30) @Max(7200) long waitingTtlSeconds,
        @Min(30) @Max(600) long enteredTtlSeconds,
        @Min(1) @Max(50) int capacity
//        @Min(1)  @Max(10000) int admissionPerSec
) {}
