package com.gomdol.concert.concert.infra.cache;

import com.gomdol.concert.common.TestContainerConfig;
import com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis 기반 랭킹 Repository 테스트")
@Import(TestContainerConfig.class)
class RedisFastSellingRankingRepositoryTest {

    @Autowired
    private FastSellingRankingRepository rankingRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String RANKING_KEY = "ranking:{concerts}:fast-selling";

    @BeforeEach
    void setUp() {
        // 각 테스트 전 Redis 데이터 초기화
        redisTemplate.keys("*").forEach(key -> redisTemplate.delete(key));
        log.info("Redis 데이터 초기화 완료");
    }

    @Test
    @DisplayName("콘서트 랭킹 점수를 업데이트한다")
    void updateConcertScore() {
        // given
        Long concertId = 1L;
        double score = 150.5;

        // when
        rankingRepository.updateConcertScore(concertId, score);

        // then
        Double savedScore = redisTemplate.opsForZSet().score(RANKING_KEY, concertId.toString());
        assertThat(savedScore).isEqualTo(score);
        log.info("콘서트 점수 업데이트 성공 - concertId={}, score={}", concertId, savedScore);
    }

    @Test
    @DisplayName("여러 콘서트의 점수를 업데이트하고 순위를 조회한다")
    void updateMultipleConcertScores() {
        // given
        rankingRepository.updateConcertScore(1L, 100.0);
        rankingRepository.updateConcertScore(2L, 200.0);
        rankingRepository.updateConcertScore(3L, 150.0);

        // when
        List<Long> topConcerts = rankingRepository.getTopConcertIds(3);

        // then
        assertThat(topConcerts).hasSize(3);
        assertThat(topConcerts).containsExactly(2L, 3L, 1L); // 점수 높은 순
        log.info("상위 콘서트 목록: {}", topConcerts);
    }

    @Test
    @DisplayName("상위 N개 콘서트 ID를 조회한다")
    void getTopConcertIds() {
        // given
        rankingRepository.updateConcertScore(1L, 50.0);
        rankingRepository.updateConcertScore(2L, 150.0);
        rankingRepository.updateConcertScore(3L, 100.0);
        rankingRepository.updateConcertScore(4L, 200.0);
        rankingRepository.updateConcertScore(5L, 75.0);

        // when
        List<Long> top3 = rankingRepository.getTopConcertIds(3);

        // then
        assertThat(top3).hasSize(3);
        assertThat(top3).containsExactly(4L, 2L, 3L); // 200 > 150 > 100
        log.info("상위 3개 콘서트: {}", top3);
    }

    @Test
    @DisplayName("랭킹 데이터가 없을 때 빈 목록을 반환한다")
    void getTopConcertIds_EmptyRanking() {
        // when
        List<Long> topConcerts = rankingRepository.getTopConcertIds(10);
        // then
        assertThat(topConcerts).isEmpty();
    }

    @Test
    @DisplayName("콘서트의 점수를 조회한다")
    void getConcertScore() {
        // given
        Long concertId = 1L;
        double expectedScore = 123.45;
        rankingRepository.updateConcertScore(concertId, expectedScore);

        // when
        Optional<Double> score = rankingRepository.getConcertScore(concertId);

        // then
        assertThat(score).isPresent();
        assertThat(score.get()).isEqualTo(expectedScore);
        log.info("콘서트 점수 조회 성공 - concertId={}, score={}", concertId, score.get());
    }

    @Test
    @DisplayName("존재하지 않는 콘서트의 점수 조회시 Empty를 반환한다")
    void getConcertScore_NotFound() {
        // when
        Optional<Double> score = rankingRepository.getConcertScore(999L);
        // then
        assertThat(score).isEmpty();
    }

    @Test
    @DisplayName("콘서트의 순위를 조회한다")
    void getConcertRank() {
        // given
        rankingRepository.updateConcertScore(1L, 50.0);
        rankingRepository.updateConcertScore(2L, 150.0);
        rankingRepository.updateConcertScore(3L, 100.0);

        // when
        Optional<Long> rank1 = rankingRepository.getConcertRank(2L); // 1위 (150점)
        Optional<Long> rank2 = rankingRepository.getConcertRank(3L); // 2위 (100점)
        Optional<Long> rank3 = rankingRepository.getConcertRank(1L); // 3위 (50점)

        // then
        assertThat(rank1).isPresent();
        assertThat(rank1.get()).isEqualTo(0); // 0부터 시작
        assertThat(rank2).isPresent();
        assertThat(rank2.get()).isEqualTo(1);
        assertThat(rank3).isPresent();
        assertThat(rank3.get()).isEqualTo(2);
        log.info("콘서트 순위 - concert2={}, concert3={}, concert1={}", rank1.get(), rank2.get(), rank3.get());
    }

    @Test
    @DisplayName("존재하지 않는 콘서트의 순위 조회시 Empty를 반환한다")
    void getConcertRank_NotFound() {
        // when
        Optional<Long> rank = rankingRepository.getConcertRank(999L);
        // then
        assertThat(rank).isEmpty();
    }

    @Test
    @DisplayName("콘서트 통계를 저장하고 조회한다")
    void saveConcertStats() {
        // given
        Long concertId = 1L;
        String concertTitle = "BTS 월드투어";
        int totalSeats = 1000;
        int reservedSeats = 750;

        // when
        rankingRepository.saveConcertStats(concertId, concertTitle, totalSeats, reservedSeats);

        // then
        Map<Long,FastSellingRankingRepository.ConcertStats> stats = rankingRepository.getConcertStats(List.of(concertId));
        assertThat(stats).hasSize(1);

        FastSellingRankingRepository.ConcertStats concertStats = stats.get(concertId);
        assertThat(concertStats.concertTitle()).isEqualTo(concertTitle);
        assertThat(concertStats.totalSeats()).isEqualTo(totalSeats);
        assertThat(concertStats.reservedSeats()).isEqualTo(reservedSeats);
        log.info("콘서트 통계 조회 성공: {}", concertStats);
    }

    @Test
    @DisplayName("여러 콘서트의 통계를 조회한다")
    void getConcertStats() {
        // given
        rankingRepository.saveConcertStats(1L, "콘서트A", 1000, 500);
        rankingRepository.saveConcertStats(2L, "콘서트B", 2000, 1500);
        rankingRepository.saveConcertStats(3L, "콘서트C", 500, 450);

        // when
        Map<Long,FastSellingRankingRepository.ConcertStats> stats = rankingRepository.getConcertStats(List.of(1L, 2L, 3L));

        // then
        assertThat(stats).hasSize(3);
        assertThat(stats.get(1L).concertTitle()).isEqualTo("콘서트A");
        assertThat(stats.get(2L).concertTitle()).isEqualTo("콘서트B");
        assertThat(stats.get(3L).concertTitle()).isEqualTo("콘서트C");
        log.info("여러 콘서트 통계 조회 성공: {}", stats.keySet());
    }

    @Test
    @DisplayName("존재하지 않는 콘서트의 통계 조회시 빈 통계를 반환한다")
    void getConcertStats_NotFound() {
        // when
        Map<Long,FastSellingRankingRepository.ConcertStats> stats = rankingRepository.getConcertStats(List.of(999L));

        // then
        assertThat(stats).hasSize(1);
        FastSellingRankingRepository.ConcertStats concertStats = stats.get(999L);
        assertThat(concertStats.concertTitle()).isEmpty();
        assertThat(concertStats.totalSeats()).isZero();
        assertThat(concertStats.reservedSeats()).isZero();
    }

    @Test
    @DisplayName("시간당 판매량을 증가시킨다")
    void incrementHourlySales() {
        // given
        Long concertId = 1L;

        // when
        rankingRepository.incrementHourlySales(concertId);
        rankingRepository.incrementHourlySales(concertId);
        rankingRepository.incrementHourlySales(concertId);

        // then
        int sales = rankingRepository.getLastHourSales(concertId);
        assertThat(sales).isEqualTo(3);
        log.info("시간당 판매량: {}", sales);
    }

    @Test
    @DisplayName("최근 1시간 판매량을 조회한다")
    void getLastHourSales() {
        // given
        Long concertId = 1L;
        rankingRepository.incrementHourlySales(concertId);
        rankingRepository.incrementHourlySales(concertId);
        rankingRepository.incrementHourlySales(concertId);
        rankingRepository.incrementHourlySales(concertId);
        rankingRepository.incrementHourlySales(concertId);

        // when
        int sales = rankingRepository.getLastHourSales(concertId);

        // then
        assertThat(sales).isEqualTo(5);
    }

    @Test
    @DisplayName("판매량이 없는 콘서트는 0을 반환한다")
    void getLastHourSales_NoSales() {
        // when
        int sales = rankingRepository.getLastHourSales(999L);

        // then
        assertThat(sales).isZero();
    }

    @Test
    @DisplayName("여러 콘서트의 시간당 판매량을 개별적으로 관리한다")
    void incrementHourlySales_MultipleConcerts() {
        // given
        Long concert1 = 1L;
        Long concert2 = 2L;
        Long concert3 = 3L;

        // when
        rankingRepository.incrementHourlySales(concert1);
        rankingRepository.incrementHourlySales(concert1);

        rankingRepository.incrementHourlySales(concert2);
        rankingRepository.incrementHourlySales(concert2);
        rankingRepository.incrementHourlySales(concert2);
        rankingRepository.incrementHourlySales(concert2);

        rankingRepository.incrementHourlySales(concert3);

        // then
        assertThat(rankingRepository.getLastHourSales(concert1)).isEqualTo(2);
        assertThat(rankingRepository.getLastHourSales(concert2)).isEqualTo(4);
        assertThat(rankingRepository.getLastHourSales(concert3)).isEqualTo(1);
    }

    @Test
    @DisplayName("통합 시나리오: 랭킹 점수 업데이트와 통계 조회")
    void integrationTest_RankingWithStats() {
        // given
        Long concert1 = 1L;
        Long concert2 = 2L;
        Long concert3 = 3L;

        // 통계 저장
        rankingRepository.saveConcertStats(concert1, "뉴진스 콘서트", 1000, 800);
        rankingRepository.saveConcertStats(concert2, "아이브 콘서트", 2000, 1900);
        rankingRepository.saveConcertStats(concert3, "BTS 콘서트", 5000, 4500);

        // 판매량 증가
        for (int i = 0; i < 10; i++)
            rankingRepository.incrementHourlySales(concert1);
        for (int i = 0; i < 50; i++)
            rankingRepository.incrementHourlySales(concert2);
        for (int i = 0; i < 30; i++)
            rankingRepository.incrementHourlySales(concert3);

        // 랭킹 점수 업데이트 (판매율 + 판매속도 * 10)
        rankingRepository.updateConcertScore(concert1, 80.0 + 10 * 10); // 180
        rankingRepository.updateConcertScore(concert2, 95.0 + 50 * 10); // 595
        rankingRepository.updateConcertScore(concert3, 90.0 + 30 * 10); // 390

        // when
        List<Long> topConcerts = rankingRepository.getTopConcertIds(3);
        Map<Long,FastSellingRankingRepository.ConcertStats> stats = rankingRepository.getConcertStats(topConcerts);

        // then
        assertThat(topConcerts).containsExactly(concert2, concert3, concert1); // 점수 순서
        assertThat(stats.get(concert2).lastHourSales()).isEqualTo(50);
        assertThat(stats.get(concert3).lastHourSales()).isEqualTo(30);
        assertThat(stats.get(concert1).lastHourSales()).isEqualTo(10);

        // 순위 확인
        assertThat(rankingRepository.getConcertRank(concert2).get()).isEqualTo(0); // 1위
        assertThat(rankingRepository.getConcertRank(concert3).get()).isEqualTo(1); // 2위
        assertThat(rankingRepository.getConcertRank(concert1).get()).isEqualTo(2); // 3위

        log.info("=== 통합 테스트 결과 ===");
        log.info("상위 콘서트 순위: {}", topConcerts);
        topConcerts.forEach(id -> {
            var stat = stats.get(id);
            var score = rankingRepository.getConcertScore(id);
            var rank = rankingRepository.getConcertRank(id);
            log.info("콘서트 ID={}, 제목={}, 점수={}, 순위={}, 시간당판매={}", id, stat.concertTitle(), score.get(), rank.get() + 1, stat.lastHourSales());
        });
    }
}
