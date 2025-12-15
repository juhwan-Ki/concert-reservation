package com.gomdol.concert.concert.infra.cache;

import com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis 기반 랭킹 Repository
 * - Sorted Set을 사용한 랭킹 관리
 * - Hash를 사용한 통계 관리
 * - 콘서트 단위로 랭킹 집계
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisFastSellingRankingRepository implements FastSellingRankingRepository {

    private final StringRedisTemplate redisTemplate;

    // 랭킹 관련 키들을 같은 슬롯에 배치 (hash tag 사용)
    private static final String RANKING_KEY = "ranking:{concerts}:fast-selling";
    private static final String STATS_KEY_PREFIX = "concert:{concerts}:stats:";
    private static final String HOURLY_KEY_PREFIX = "concert:{concerts}:sales:hourly:";
    private static final Duration RANKING_TTL = Duration.ofHours(24);
    private static final Duration STATS_TTL = Duration.ofHours(1);
    private static final Duration HOURLY_TTL = Duration.ofHours(2);

    @Override
    public void updateConcertScore(Long concertId, double score) {
        try {
            redisTemplate.opsForZSet().add(RANKING_KEY, concertId.toString(), score);
            redisTemplate.expire(RANKING_KEY, RANKING_TTL);
            log.debug("콘서트 랭킹 점수 업데이트 - concertId={}, score={}", concertId, score);
        } catch (Exception e) {
            log.error("콘서트 랭킹 점수 업데이트 실패 - concertId={}", concertId, e);
        }
    }

    @Override
    public void incrementHourlySales(Long concertId) {
        try {
            String hourlyKey = HOURLY_KEY_PREFIX + concertId;
            long currentHour = System.currentTimeMillis() / 3600000;

            // 현재 시간대에 판매량 증가
            redisTemplate.opsForHash().increment(hourlyKey, String.valueOf(currentHour), 1);

            // 1시간 이전 데이터 삭제
            Map<Object, Object> allSales = redisTemplate.opsForHash().entries(hourlyKey);
            allSales.keySet().forEach(field -> {
                long hour = Long.parseLong(field.toString());
                if (hour < currentHour) {
                    redisTemplate.opsForHash().delete(hourlyKey, field);
                }
            });

            redisTemplate.expire(hourlyKey, HOURLY_TTL);
            log.debug("시간당 판매량 증가 - concertId={}, hour={}", concertId, currentHour);
        } catch (Exception e) {
            log.error("시간당 판매량 증가 실패 - concertId={}", concertId, e);
        }
    }

    @Override
    public List<Long> getTopConcertIds(int topNum) {
        try {
            Set<String> results = redisTemplate.opsForZSet().reverseRange(RANKING_KEY, 0, topNum - 1);

            if (results == null || results.isEmpty())
                return List.of();

            return results.stream().map(Long::parseLong).toList();
        } catch (Exception e) {
            log.error("상위 콘서트 ID 조회 실패 - topN={}", topNum, e);
            return List.of();
        }
    }

    @Override
    public Optional<Double> getConcertScore(Long concertId) {
        try {
            Double score = redisTemplate.opsForZSet().score(RANKING_KEY, concertId.toString());
            return Optional.ofNullable(score);
        } catch (Exception e) {
            log.error("콘서트 점수 조회 실패 - concertId={}", concertId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Long> getConcertRank(Long concertId) {
        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, concertId.toString());
            return Optional.ofNullable(rank);
        } catch (Exception e) {
            log.error("콘서트 순위 조회 실패 - concertId={}", concertId, e);
            return Optional.empty();
        }
    }

    @Override
    public Map<Long, ConcertStats> getConcertStats(List<Long> concertIds) {
        try {
            return concertIds.stream().collect(Collectors.toMap(concertId -> concertId, this::getConcertStatsForId));
        } catch (Exception e) {
            log.error("콘서트 통계 조회 실패 - concertIds={}", concertIds, e);
            return Map.of();
        }
    }

    @Override
    public void saveConcertStats(Long concertId, String concertTitle, int totalSeats, int reservedSeats) {
        try {
            String statsKey = STATS_KEY_PREFIX + concertId;
            Map<String, String> stats = new HashMap<>();
            stats.put("concertTitle", concertTitle);
            stats.put("totalSeats", String.valueOf(totalSeats));
            stats.put("reservedSeats", String.valueOf(reservedSeats));

            redisTemplate.opsForHash().putAll(statsKey, stats);
            redisTemplate.expire(statsKey, STATS_TTL);
            log.debug("콘서트 통계 저장 - concertId={}, title={}, totalSeats={}, reservedSeats={}", concertId, concertTitle, totalSeats, reservedSeats);
        } catch (Exception e) {
            log.error("콘서트 통계 저장 실패 - concertId={}", concertId, e);
        }
    }

    @Override
    public int getLastHourSales(Long concertId) {
        try {
            String hourlyKey = HOURLY_KEY_PREFIX + concertId;
            Map<Object, Object> allSales = redisTemplate.opsForHash().entries(hourlyKey);

            if (allSales.isEmpty())
                return 0;

            return allSales.values().stream().mapToInt(value -> Integer.parseInt(value.toString())).sum();
        } catch (Exception e) {
            log.error("최근 1시간 판매량 조회 실패 - concertId={}", concertId, e);
            return 0;
        }
    }

    /**
     * 단일 콘서트의 통계 조회
     */
    private ConcertStats getConcertStatsForId(Long concertId) {
        try {
            String statsKey = STATS_KEY_PREFIX + concertId;
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(statsKey);

            if (stats.isEmpty())
                return new ConcertStats("",0, 0, 0);

            String concertTitle = stats.getOrDefault("concertTitle", "").toString();
            int totalSeats = Integer.parseInt(stats.getOrDefault("totalSeats", "0").toString());
            int reservedSeats = Integer.parseInt(stats.getOrDefault("reservedSeats", "0").toString());
            int lastHourSales = getLastHourSales(concertId);

            return new ConcertStats(concertTitle, totalSeats, reservedSeats, lastHourSales);
        } catch (Exception e) {
            log.error("콘서트 통계 조회 실패 - concertId={}", concertId, e);
            return new ConcertStats("",0, 0, 0);
        }
    }
}
