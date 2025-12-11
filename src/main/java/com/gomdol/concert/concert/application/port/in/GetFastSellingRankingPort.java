package com.gomdol.concert.concert.application.port.in;

import com.gomdol.concert.concert.domain.model.FastSellingConcert;

import java.util.List;
import java.util.Optional;

/**
 * 랭킹 조회 Port
 * - 상위 N개 랭킹 조회
 * - 특정 공연의 랭킹 조회
 */
public interface GetFastSellingRankingPort {

    /**
     * 상위 N개 콘서트 조회
     *
     * @param topNum 조회할 상위 개수
     * @return 콘서트 목록 (순위 포함)
     */
    List<FastSellingConcert> getTopRanking(int topNum);

    /**
     * 특정 콘서트의 랭킹 정보 조회
     *
     * @param concertId 콘서트 ID
     * @return 콘서트의 랭킹 정보
     */
    Optional<FastSellingConcert> getConcertRanking(Long concertId);
}
