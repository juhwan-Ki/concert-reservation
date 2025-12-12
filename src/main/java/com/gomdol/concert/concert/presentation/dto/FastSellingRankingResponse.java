package com.gomdol.concert.concert.presentation.dto;

import com.gomdol.concert.concert.domain.model.FastSellingConcert;

import java.util.List;

/**
 * 랭킹 응답 DTO
 * - 콘서트 단위로 랭킹 표시
 */
public record FastSellingRankingResponse(List<FastSellingConcertDto> rankings) {
    public static FastSellingRankingResponse of(List<FastSellingConcert> concerts) {
        List<FastSellingConcertDto> list = concerts.stream().map(FastSellingConcertDto::from).toList();
        return new FastSellingRankingResponse(list);
    }

    /**
     * 콘서트 DTO
     */
    public record FastSellingConcertDto(
            Long concertId,
            String concertTitle,
            int totalSeats,
            int reservedSeats,
            int availableSeats,
            double salesRate,
            double salesSpeed,
            int rank,
            boolean soldOut
    ) {
        public static FastSellingConcertDto from(FastSellingConcert concert) {
            return new FastSellingConcertDto(
                    concert.getConcertId(),
                    concert.getConcertTitle(),
                    concert.getTotalSeats(),
                    concert.getReservedSeats(),
                    concert.availableSeats(),
                    Math.round(concert.getSalesRate() * 100) / 100.0,  // 소수점 2자리
                    Math.round(concert.getSalesSpeed() * 100) / 100.0,
                    concert.getRank(),
                    concert.isSoldOut()
            );
        }
    }
}
