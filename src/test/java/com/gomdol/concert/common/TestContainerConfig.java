package com.gomdol.concert.common;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 기반 테스트 환경 설정
 * - Redis, MySQL 컨테이너를 모든 테스트에서 공유
 * - 컨테이너 재사용으로 테스트 실행 속도 향상
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {

    /**
     * Redis 컨테이너
     * - static 필드로 모든 테스트에서 공유
     * - @Container로 Testcontainers 생명주기 관리
     */
    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    static {
        // 클래스 로딩 시점에 Redis 컨테이너 시작
        redisContainer.start();
    }

    /**
     * MySQL 컨테이너
     * - @ServiceConnection으로 자동 연결 설정
     */
    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("test_db")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
    }

    /**
     * 테스트용 RedisConnectionFactory
     * - Testcontainers Redis에 연결
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisContainer.getHost());
        config.setPort(redisContainer.getMappedPort(6379));
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * 테스트용 RedissonClient
     * - Testcontainers Redis에 연결
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379));
        return Redisson.create(config);
    }

    /**
     * Redis 속성을 동적으로 등록
     * - Spring Data Redis 자동 설정에서 사용
     * - 애플리케이션 설정을 오버라이드
     */
    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }
}