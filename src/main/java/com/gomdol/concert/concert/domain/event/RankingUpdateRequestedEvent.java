package com.gomdol.concert.concert.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 랭킹 업데이트 요청 이벤트
 * - 좌석 확정 시 발행되는 이벤트
 * - 랭킹 업데이트에 필요한 모든 정보 포함
 */
@Getter
public class RankingUpdateRequestedEvent {

    private final Long reservationId;
    private final Long concertId;
    private final String concertTitle;
    private final int totalSeats;       // 전체 좌석 수
    private final int reservedSeats;    // 예약된 좌석 수
    private final int seatCount;        // 이번 예약 좌석 수
    private final LocalDateTime occurredAt;

    private RankingUpdateRequestedEvent(Long reservationId, Long concertId, String concertTitle, int totalSeats, int reservedSeats, int seatCount) {
        this.reservationId = reservationId;
        this.concertId = concertId;
        this.concertTitle = concertTitle;
        this.totalSeats = totalSeats;
        this.reservedSeats = reservedSeats;
        this.seatCount = seatCount;
        this.occurredAt = LocalDateTime.now();
    }

    public static RankingUpdateRequestedEvent of(Long reservationId, Long concertId, String concertTitle, int totalSeats, int reservedSeats, int seatCount) {
        return new RankingUpdateRequestedEvent(reservationId, concertId, concertTitle, totalSeats, reservedSeats, seatCount);
    }
}
