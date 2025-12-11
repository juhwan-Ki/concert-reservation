package com.gomdol.concert.concert.domain.model;

import lombok.Getter;

/**
 * 콘서트 랭킹 도메인
 * - 콘서트 단위로 판매 속도와 판매율을 기반으로 랭킹 계산
 * - 같은 콘서트의 여러 공연 날짜를 하나로 집계
 */
@Getter
public class FastSellingConcert {

    private final Long concertId;
    private final String concertTitle;
    private final int totalSeats;
    private final int reservedSeats;
    private final double salesRate;        // 판매율 (%)
    private final double salesSpeed;       // 판매속도 (건/시간)
    private final double rankingScore;     // 랭킹 점수
    private final int rank;                // 순위

    private FastSellingConcert(
            Long concertId, String concertTitle, int totalSeats,
            int reservedSeats, double salesRate, double salesSpeed, double rankingScore, int rank)
    {
        this.concertId = concertId;
        this.concertTitle = concertTitle;
        this.totalSeats = totalSeats;
        this.reservedSeats = reservedSeats;
        this.salesRate = salesRate;
        this.salesSpeed = salesSpeed;
        this.rankingScore = rankingScore;
        this.rank = rank;
    }

    public static FastSellingConcert of(Long concertId, String concertTitle, int totalSeats, int reservedSeats, int lastHourSales) {
        double salesRate = totalSeats > 0 ? (double) reservedSeats / totalSeats * 100 : 0;
        double rankingScore = calculateScore(salesRate, lastHourSales);

        return new FastSellingConcert(concertId, concertTitle, totalSeats, reservedSeats, salesRate, lastHourSales, rankingScore, 0);
    }

    /**
     * 순위 정보를 포함
     */
    public FastSellingConcert withRank(int rank) {
        return new FastSellingConcert(concertId, concertTitle, totalSeats, reservedSeats, salesRate, salesSpeed, rankingScore, rank);
    }

    /**
     * 랭킹 점수 계산
     * 점수 = 판매율 + (판매속도 × 가중치)
     *
     * @param salesRate  판매율 (%)
     * @param salesSpeed 판매속도 (건/시간)
     * @return 랭킹 점수
     */
    private static double calculateScore(double salesRate, double salesSpeed) {
        final double SPEED_WEIGHT = 10.0;
        return salesRate + (salesSpeed * SPEED_WEIGHT);
    }

    /**
     * 매진 여부 확인
     */
    public boolean isSoldOut() {
        return reservedSeats >= totalSeats;
    }

    /**
     * 남은 좌석 수
     */
    public int availableSeats() {
        return Math.max(0, totalSeats - reservedSeats);
    }
}
