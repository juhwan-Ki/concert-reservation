package com.gomdol.concert.concert.domain;

import com.gomdol.concert.concert.domain.model.FastSellingConcert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FastSellingConcert 도메인 모델 테스트
 */
class FastSellingConcertTest {

    @Test
    @DisplayName("콘서트 랭킹 정보를 생성한다")
    void createFastSellingConcert() {
        // given
        Long concertId = 1L;
        String concertTitle = "아이유 콘서트 2025";
        int totalSeats = 1000;
        int reservedSeats = 500;
        int lastHourSales = 50;

        // when
        FastSellingConcert result = FastSellingConcert.of(concertId, concertTitle, totalSeats, reservedSeats, lastHourSales);

        // then
        assertThat(result.getConcertId()).isEqualTo(concertId);
        assertThat(result.getConcertTitle()).isEqualTo(concertTitle);
        assertThat(result.getTotalSeats()).isEqualTo(totalSeats);
        assertThat(result.getReservedSeats()).isEqualTo(reservedSeats);
        assertThat(result.getSalesSpeed()).isEqualTo(lastHourSales);
    }

    @Test
    @DisplayName("판매율을 정확하게 계산한다")
    void calculateSalesRate() {
        // given
        int totalSeats = 1000;
        int reservedSeats = 500;

        // when
        FastSellingConcert result = FastSellingConcert.of(1L, "콘서트", totalSeats, reservedSeats, 10);

        // then
        assertThat(result.getSalesRate()).isEqualTo(50.0); // 500/1000 * 100 = 50%
    }

    @Test
    @DisplayName("랭킹 점수를 정확하게 계산한다 - 판매율 + 판매속도*10")
    void calculateRankingScore() {
        // given
        int totalSeats = 1000;
        int reservedSeats = 500;  // 판매율 50%
        int lastHourSales = 20;    // 판매속도 20건/시간

        // when
        FastSellingConcert result = FastSellingConcert.of(1L, "콘서트", totalSeats, reservedSeats, lastHourSales);

        // then
        // 점수 = 50 + (20 * 10) = 250
        assertThat(result.getRankingScore()).isEqualTo(250.0);
    }

    @Test
    @DisplayName("전체 좌석이 0일 때 판매율은 0이다")
    void salesRateIsZeroWhenTotalSeatsIsZero() {
        // when
        FastSellingConcert result = FastSellingConcert.of(1L, "콘서트", 0, 0, 10);

        // then
        assertThat(result.getSalesRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("매진 여부를 정확하게 판단한다 - 예약 좌석이 전체 좌석과 같으면 매진")
    void isSoldOut_WhenReservedEqualsTotal() {
        // when
        FastSellingConcert result = FastSellingConcert.of(1L, "콘서트", 1000, 1000, 10);

        // then
        assertThat(result.isSoldOut()).isTrue();
    }

    @Test
    @DisplayName("매진 여부를 정확하게 판단한다 - 예약 좌석이 전체 좌석보다 많아도 매진")
    void isSoldOut_WhenReservedExceedsTotal() {
        // when
        FastSellingConcert result = FastSellingConcert.of(1L, "콘서트", 1000, 1100, 10);

        // then
        assertThat(result.isSoldOut()).isTrue();
    }

    @Test
    @DisplayName("매진 여부를 정확하게 판단한다 - 남은 좌석이 있으면 매진 아님")
    void isSoldOut_WhenSeatsAvailable() {
        // when
        FastSellingConcert result = FastSellingConcert.of(1L, "콘서트", 1000, 500, 10);

        // then
        assertThat(result.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("남은 좌석 수를 정확하게 계산한다")
    void availableSeats() {
        // when
        FastSellingConcert result = FastSellingConcert.of(1L, "콘서트", 1000, 300, 10);

        // then
        assertThat(result.availableSeats()).isEqualTo(700);
    }

    @Test
    @DisplayName("남은 좌석이 음수가 되지 않는다")
    void availableSeatsNotNegative() {
        // when
        FastSellingConcert result = FastSellingConcert.of(1L, "콘서트", 1000, 1200, 10);

        // then
        assertThat(result.availableSeats()).isEqualTo(0);
    }

    @Test
    @DisplayName("순위를 설정할 수 있다")
    void withRank() {
        // given
        FastSellingConcert original = FastSellingConcert.of(1L, "콘서트", 1000, 500, 10);

        // when
        FastSellingConcert withRank = original.withRank(1);

        // then
        assertThat(withRank.getRank()).isEqualTo(1);
        assertThat(withRank.getConcertId()).isEqualTo(original.getConcertId());
        assertThat(withRank.getSalesRate()).isEqualTo(original.getSalesRate());
    }

    @Test
    @DisplayName("판매속도가 높을수록 랭킹 점수가 높다")
    void higherSalesSpeedMeansHigherScore() {
        // given
        FastSellingConcert slow = FastSellingConcert.of(1L, "느린 콘서트", 1000, 500, 10);
        FastSellingConcert fast = FastSellingConcert.of(2L, "빠른 콘서트", 1000, 500, 50);

        // then
        assertThat(fast.getRankingScore()).isGreaterThan(slow.getRankingScore());
    }

    @Test
    @DisplayName("판매율이 높을수록 랭킹 점수가 높다")
    void higherSalesRateMeansHigherScore() {
        // given
        FastSellingConcert low = FastSellingConcert.of(1L, "저조한 콘서트", 1000, 300, 10);
        FastSellingConcert high = FastSellingConcert.of(2L, "인기있는 콘서트", 1000, 800, 10);

        // then
        assertThat(high.getRankingScore()).isGreaterThan(low.getRankingScore());
    }
}
