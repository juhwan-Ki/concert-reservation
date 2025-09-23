package com.gomdol.concert.reservation.domain.model;

import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import lombok.Getter;

@Getter
public class ReservationSeat {

    private final Long id;
    private final Long reservationId;
    private final Long seatId;           // ID로만 참조
    private final Long showId;           // ID로만 참조
    private final ReservationSeatStatus status;

    private ReservationSeat(Long id, Long reservationId, Long seatId, Long showId, ReservationSeatStatus status) {
        this.id = id;
        this.reservationId = reservationId;
        this.seatId = seatId;
        this.showId = showId;
        this.status = status;
    }

    // 팩토리 메서드
    public static ReservationSeat create(Long reservationId, Long seatId, Long showId) {
        return new ReservationSeat(null, reservationId, seatId, showId, ReservationSeatStatus.HOLD);
    }

    public static ReservationSeat of(Long id, Long reservationId, Long seatId, Long showId, ReservationSeatStatus status) {
        return new ReservationSeat(id, reservationId, seatId, showId, status);
    }

    // 상태 변경 메서드
    public ReservationSeat confirm() {
        validateCanConfirm();
        return new ReservationSeat(id, reservationId, seatId, showId, ReservationSeatStatus.CONFIRMED);
    }

    public ReservationSeat cancel() {
        validateCanCancel();
        return new ReservationSeat(id, reservationId, seatId, showId, ReservationSeatStatus.CANCELED);
    }

    public ReservationSeat expire() {
        if (this.status == ReservationSeatStatus.HOLD)
            return new ReservationSeat(id, reservationId, seatId, showId, ReservationSeatStatus.EXPIRED);

        return this;
    }

    private void validateCanConfirm() {
        if (this.status != ReservationSeatStatus.HOLD)
            throw new IllegalStateException("HOLD 상태의 좌석만 확정할 수 있습니다.");
    }

    private void validateCanCancel() {
        if (this.status == ReservationSeatStatus.CANCELED || this.status == ReservationSeatStatus.EXPIRED)
            throw new IllegalStateException("이미 취소되거나 만료된 좌석입니다.");
    }

    public boolean isConfirmed() { return this.status == ReservationSeatStatus.CONFIRMED; }
    public boolean isHold() { return this.status == ReservationSeatStatus.HOLD; }
    public boolean isCanceled() { return this.status == ReservationSeatStatus.CANCELED; }
}
