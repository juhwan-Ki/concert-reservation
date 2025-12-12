package com.gomdol.concert.common.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 설정
 * - StringRedisTemplate: String 기반 Redis 작업
 * - ObjectMapper: JSON 직렬화/역직렬화
 * - DistributedLockProperties: 분산 락 설정 활성화
 */
@Configuration
@EnableConfigurationProperties(DistributedLockProperties.class)
public class RedisConfig {

    /**
     * StringRedisTemplate 생성
     * - Key/Value 모두 String으로 처리
     * - JSON 직렬화는 ObjectMapper가 담당
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * ObjectMapper 생성
     * - JavaTimeModule: LocalDateTime 등 Java 8 시간 API 지원
     * - WRITE_DATES_AS_TIMESTAMPS: false → ISO-8601 형식으로 직렬화
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}