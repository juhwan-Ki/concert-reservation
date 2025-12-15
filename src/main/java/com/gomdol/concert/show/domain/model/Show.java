package com.gomdol.concert.show.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Show {
    private final Long id;
    private final Long concertId;
    private final ShowStatus status;
    private final LocalDateTime showAt;
    private final String concertTitle;
    private final String venue;
    private final int capacity;
    private final int reservationCnt;

    private Show(Long id, Long concertId, ShowStatus status, LocalDateTime showAt, String concertTitle, String venue, int capacity, int reservationCnt) {
        this.id = id;
        this.concertId = concertId;
        this.status = status;
        this.showAt = showAt;
        this.concertTitle = concertTitle;
        this.venue = venue;
        this.capacity = capacity;
        this.reservationCnt = reservationCnt;
    }

    public static Show create(Long id, Long concertId, ShowStatus status, LocalDateTime showAt, String concertTitle, String venue, int capacity, int reservationCnt) {
        return new Show(id, concertId, status, showAt, concertTitle, venue, capacity, reservationCnt);
    }

    /**
     * 전체 좌석 수
     */
    public int getTotalSeats() {
        return capacity;
    }

    /**
     * 예약된 좌석 수
     */
    public int getReservedSeats() {
        return reservationCnt;
    }

}
