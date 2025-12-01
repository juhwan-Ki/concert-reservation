package com.gomdol.concert.common.application.cache.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * 캐시 저장소 Output Port
 * - Redis 등 다양한 캐시 구현체로 교체 가능
 */
public interface CacheRepository {

    /**
     * 캐시에서 값 조회
     *
     * @param key 캐시 키
     * @param valueType 값의 타입
     * @param <T> 반환 타입
     * @return 캐시된 값 (없으면 empty)
     */
    <T> Optional<T> get(String key, Class<T> valueType);

    /**
     * 캐시에 값 저장 (TTL 포함)
     *
     * @param key 캐시 키
     * @param value 저장할 값
     * @param ttl 만료 시간
     * @param <T> 값의 타입
     */
    <T> void set(String key, T value, Duration ttl);

    /**
     * 캐시에서 값 삭제
     *
     * @param key 캐시 키
     */
    void delete(String key);

    /**
     * 캐시 존재 여부 확인
     *
     * @param key 캐시 키
     * @return 존재 여부
     */
    boolean exists(String key);
}