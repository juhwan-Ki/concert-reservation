package com.gomdol.concert.common.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "distributed-lock")
public record DistributedLockProperties(
        LockConfig reservation,
        LockConfig payment,
        LockConfig point
) {
    public record LockConfig(Duration waitTime, Duration leaseTime) {}
}
