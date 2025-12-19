package com.gomdol.concert.reservation.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 완료 도메인
 * 예약이 확정(결제 완료)되었을 때 발행되는 도메인
 */
@Getter
public class ReservationCompletedEvent {

    private final Long reservationId;
    private final String userId;
    private final String reservationCode;
    private final List<SeatInfo> seats;
    private final long totalAmount;
    private final LocalDateTime confirmedAt;
    private final LocalDateTime occurredAt;

    private ReservationCompletedEvent(Long reservationId, String userId, String reservationCode, List<SeatInfo> seats, long totalAmount, LocalDateTime confirmedAt) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.reservationCode = reservationCode;
        this.seats = seats;
        this.totalAmount = totalAmount;
        this.confirmedAt = confirmedAt;
        this.occurredAt = LocalDateTime.now();
    }

    public static ReservationCompletedEvent of(Long reservationId, String userId, String reservationCode, List<SeatInfo> seats, long totalAmount, LocalDateTime confirmedAt) {
        return new ReservationCompletedEvent(reservationId, userId, reservationCode, seats, totalAmount, confirmedAt);
    }

    public record SeatInfo(Long seatId, Long showId, long price) {
        public static SeatInfo of(Long seatId, Long showId, long price) {
                return new SeatInfo(seatId, showId, price);
        }
    }
}
