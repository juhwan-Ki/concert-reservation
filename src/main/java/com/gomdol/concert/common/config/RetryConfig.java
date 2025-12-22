package com.gomdol.concert.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 설정
 * @Retryable 어노테이션을 사용하기 위한 설정
 */
@EnableRetry
@Configuration
public class RetryConfig {
}
