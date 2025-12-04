package com.gomdol.concert.common.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomdol.concert.common.application.cache.port.out.CacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 기반 캐시 구현체
 * - JSON 직렬화/역직렬화
 * - TTL 지원
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisCacheRepository implements CacheRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public <T> Optional<T> get(String key, Class<T> valueType) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("Cache miss - key: {}", key);
                return Optional.empty();
            }

            T value = objectMapper.readValue(json, valueType);
            log.debug("Cache hit - key: {}", key);
            return Optional.of(value);

        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cache value - key: {}, error: {}", key, e.getMessage());
            // 역직렬화 실패 시 캐시 삭제
            delete(key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error while getting cache - key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Cache set - key: {}, ttl: {}", key, ttl);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cache value - key: {}", key, e);
        } catch (Exception e) {
            log.error("Unexpected error while setting cache - key: {}", key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Cache delete - key: {}, deleted: {}", key, deleted);
        } catch (Exception e) {
            log.error("Error deleting cache - key: {}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Error checking cache existence - key: {}", key, e);
            return false;
        }
    }
}