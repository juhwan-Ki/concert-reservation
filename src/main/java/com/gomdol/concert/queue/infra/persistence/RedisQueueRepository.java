package com.gomdol.concert.queue.infra.persistence;

import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis 기반 대기열 Repository
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.repository", havingValue = "redis", matchIfMissing = false)
public class RedisQueueRepository implements QueueRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String WAITING_KEY = "queue:%d:waiting";
    private static final String ENTERED_KEY = "queue:%d:entered";
    private static final String TOKEN_KEY = "queue:token:%s";
    private static final String USER_TOKEN_KEY = "queue:user:%d:%s";

    @Override
    public QueueToken issueToken(Long targetId, String userId, String token, QueueStatus status, long ttlSeconds) {
        String userTokenKey = String.format(USER_TOKEN_KEY, targetId, userId);

        // 존재하지 않을 때만 설정되고, 이미 존재하면 false 반환
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(userTokenKey, token, Duration.ofSeconds(ttlSeconds + 60));
        if (Boolean.FALSE.equals(isNew)) {
            // 이미 발급된 토큰이 있음 - 기존 토큰 반환
            String existingToken = redisTemplate.opsForValue().get(userTokenKey);
            if (existingToken != null) {
                QueueToken existingQueueToken = findByToken(existingToken, targetId);
                if (existingQueueToken != null) {
                    log.debug("기존 토큰 반환 (원자적): targetId={}, userId={}, token={}", targetId, userId, existingToken);
                    return existingQueueToken;
                }
            }
        }

        // 신규 토큰 발급
        String tokenKey = String.format(TOKEN_KEY, token);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        // 토큰 정보 저장 (Hash)
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("userId", userId);
        tokenData.put("targetId", String.valueOf(targetId));
        tokenData.put("status", status.name());
        tokenData.put("createdAt", now.toString());
        tokenData.put("expiresAt", expiresAt.toString());
        tokenData.put("token", token);

        redisTemplate.opsForHash().putAll(tokenKey, tokenData);
        redisTemplate.expire(tokenKey, Duration.ofSeconds(ttlSeconds + 60)); // 여유 TTL

        String queueKey = status == QueueStatus.WAITING ? String.format(WAITING_KEY, targetId) : String.format(ENTERED_KEY, targetId);
        double score = status == QueueStatus.WAITING ? now.toEpochMilli() : expiresAt.toEpochMilli();

        redisTemplate.opsForZSet().add(queueKey, userId, score);

        // Position 계산 (WAITING인 경우)
        long position = 0;
        if (status == QueueStatus.WAITING) {
            Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);
            position = rank != null ? rank + 1 : 1;
        }

        log.info("토큰 발급: targetId={}, userId={}, status={}, position={}", targetId, userId, status, position);
        return QueueToken.create(token, userId, targetId, status, position, ttlSeconds);
    }

    @Override
    public boolean isWaiting(Long targetId) {
        String waitingKey = String.format(WAITING_KEY, targetId);
        Long size = redisTemplate.opsForZSet().size(waitingKey);
        return size != null && size > 0;
    }

    @Override
    public Optional<QueueToken> findByTargetIdAndUserId(Long targetId, String userId) {
        String userTokenKey = String.format(USER_TOKEN_KEY, targetId, userId);
        String token = redisTemplate.opsForValue().get(userTokenKey);

        if (token == null)
            return Optional.empty();

        QueueToken queueToken = findByToken(token, targetId);
        return Optional.ofNullable(queueToken);
    }

    @Override
    public Optional<QueueToken> findByTargetIdAndToken(Long targetId, String token) {
        QueueToken queueToken = findByToken(token, targetId);
        return Optional.ofNullable(queueToken);
    }

    @Override
    public List<Long> findActiveTargetIds(Instant now) {
        // Redis SCAN으로 모든 queue:*:waiting 키 조회
        Set<String> keys = new HashSet<>();
        redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            ScanOptions options = ScanOptions.scanOptions().match("queue:*:waiting").count(100).build();
            Cursor<byte[]> cursor = connection.scan(options);
            while (cursor.hasNext())
                keys.add(new String(cursor.next()));

            return keys;
        });

        return keys.stream()
                .map(key -> {
                    String[] parts = key.split(":");
                    if (parts.length >= 2) {
                        try {
                            return Long.parseLong(parts[1]);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid targetId in key: {}", key);
                            return null;
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public long countEnteredActiveWithLock(Long targetId, Instant now) {
        String enteredKey = String.format(ENTERED_KEY, targetId);

        // 만료된 항목 제거 (score < now)
        Long removed = redisTemplate.opsForZSet().removeRangeByScore(enteredKey, 0, now.toEpochMilli());
        if (removed != null && removed > 0)
            log.debug("만료된 ENTERED 토큰 {}개 정리: targetId={}", removed, targetId);

        // 남은 항목 세기 (모두 유효한 ENTERED 토큰)
        Long count = redisTemplate.opsForZSet().size(enteredKey);
        return count != null ? count : 0;
    }

    @Override
    public void save(QueueToken token) {
        // 상태 변경 (WAITING → ENTERED 승급만 처리)
        // EXPIRED는 Lazy Deletion + TTL로 자동 처리됨
        if (token.getStatus() != QueueStatus.ENTERED) {
            log.debug("ENTERED 상태가 아님 - 승급 불필요: status={}", token.getStatus());
            return;
        }

        String tokenKey = String.format(TOKEN_KEY, token.getToken());
        String waitingKey = String.format(WAITING_KEY, token.getTargetId());
        String enteredKey = String.format(ENTERED_KEY, token.getTargetId());

        Instant expiresAt = Instant.now().plusSeconds(token.getTtlSeconds());

        // 토큰 정보 업데이트
        redisTemplate.opsForHash().put(tokenKey, "status", QueueStatus.ENTERED.name());
        redisTemplate.opsForHash().put(tokenKey, "expiresAt", expiresAt.toString());

        // 대기열 상태 변경: WAITING → ENTERED
        redisTemplate.opsForZSet().remove(waitingKey, token.getUserId());
        redisTemplate.opsForZSet().add(enteredKey, token.getUserId(), expiresAt.toEpochMilli());

        // 사용자-토큰 매핑 TTL 갱신
        String userTokenKey = String.format(USER_TOKEN_KEY, token.getTargetId(), token.getUserId());
        redisTemplate.expire(userTokenKey, Duration.ofSeconds(token.getTtlSeconds() + 60));
        redisTemplate.expire(tokenKey, Duration.ofSeconds(token.getTtlSeconds() + 60));

        log.info("토큰 승급: targetId={}, userId={}, token={}", token.getTargetId(), token.getUserId(), token.getToken());
    }

    @Override
    public List<QueueToken> findAndLockWaitingTokens(Long targetId, Instant now, int limit) {
        String waitingKey = String.format(WAITING_KEY, targetId);

        // 만료되지 않은 대기 중인 사용자 조회 (score가 낮은 순 = 먼저 들어온 순)
        Set<String> userIds = redisTemplate.opsForZSet().range(waitingKey, 0, limit - 1);

        if (userIds == null || userIds.isEmpty())
            return List.of();

        return userIds.stream()
                .map(userId -> {
                    String userTokenKey = String.format(USER_TOKEN_KEY, targetId, userId);
                    String token = redisTemplate.opsForValue().get(userTokenKey);
                    if (token == null)
                        return null;
                    return findByToken(token, targetId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<QueueToken> findAllExpiredAndOffsetLimit(Instant now, int limit) {
        // Redis 버전에서는 조회 시 사용으로 스케줄러 불필요
        // countEnteredActiveWithLock()에서 조회 시 자동으로 만료 항목 정리됨
        log.debug("findAllExpiredAndOffsetLimit() - Lazy Deletion 사용으로 불필요");
        return List.of();
    }

    /**
     * 토큰으로 QueueToken 조회
     */
    private QueueToken findByToken(String token, Long targetId) {
        String tokenKey = String.format(TOKEN_KEY, token);
        Map<Object, Object> data = redisTemplate.opsForHash().entries(tokenKey);

        if (data.isEmpty())
            return null;

        String userId = (String) data.get("userId");
        String statusStr = (String) data.get("status");
        String expiresAtStr = (String) data.get("expiresAt");

        if (userId == null || statusStr == null || expiresAtStr == null) {
            log.warn("불완전한 토큰 데이터: token={}", token);
            return null;
        }

        QueueStatus status = QueueStatus.valueOf(statusStr);
        Instant expiresAt = Instant.parse(expiresAtStr);

        // TTL 계산
        long ttl = Math.max(0, Duration.between(Instant.now(), expiresAt).toSeconds());

        // TTL이 0이면 EXPIRED 처리
        if (ttl == 0 && status != QueueStatus.EXPIRED)
            status = QueueStatus.EXPIRED;

        // Position 계산
        long position = 0;
        if (status == QueueStatus.WAITING) {
            String waitingKey = String.format(WAITING_KEY, targetId);
            Long rank = redisTemplate.opsForZSet().rank(waitingKey, userId);
            position = rank != null ? rank + 1 : 0;
        }

        return QueueToken.create(token, userId, targetId, status, position, ttl);
    }
}
