package com.gomdol.concert.concert.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 랭킹 Repository Port
 * - 콘서트 단위로 랭킹 집계
 */
public interface FastSellingRankingRepository {

    /**
     * 콘서트의 랭킹 점수 업데이트
     *
     * @param concertId 콘서트 ID
     * @param score     랭킹 점수
     */
    void updateConcertScore(Long concertId, double score);

    /**
     * 콘서트의 시간당 판매량 증가
     *
     * @param concertId 콘서트 ID
     */
    void incrementHourlySales(Long concertId);

    /**
     * 상위 N개 콘서트 ID 조회
     *
     * @param topN 조회할 개수
     * @return 콘서트 ID 목록 (점수 높은 순)
     */
    List<Long> getTopConcertIds(int topN);

    /**
     * 콘서트의 랭킹 점수 조회
     *
     * @param concertId 콘서트 ID
     * @return 랭킹 점수
     */
    Optional<Double> getConcertScore(Long concertId);

    /**
     * 콘서트의 순위 조회
     *
     * @param concertId 콘서트 ID
     * @return 순위 (0부터 시작)
     */
    Optional<Long> getConcertRank(Long concertId);

    /**
     * 여러 콘서트의 통계 조회
     *
     * @param concertIds 콘서트 ID 목록
     * @return 콘서트별 통계 맵
     */
    Map<Long, ConcertStats> getConcertStats(List<Long> concertIds);

    /**
     * 콘서트 통계 저장
     *
     * @param concertId     콘서트 ID
     * @param concertTitle  콘서트 제목
     * @param totalSeats    전체 좌석 수
     * @param reservedSeats 예약된 좌석 수
     */
    void saveConcertStats(Long concertId, String concertTitle, int totalSeats, int reservedSeats);

    /**
     * 콘서트의 최근 1시간 판매량 조회
     *
     * @param concertId 콘서트 ID
     * @return 최근 1시간 판매량
     */
    int getLastHourSales(Long concertId);

    /**
     * 콘서트 통계 정보
     */
    record ConcertStats(String concertTitle, int totalSeats, int reservedSeats, int lastHourSales) {}
}
