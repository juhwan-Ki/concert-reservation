package com.gomdol.concert.common;

import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import com.gomdol.concert.venue.domain.model.VenueSeat;

import java.time.LocalDateTime;
import java.util.List;

import static com.gomdol.concert.common.FixedField.*;

public class ReservationTestFixture {

    public static Reservation mockExpireReservation() {
        return Reservation.of(1L, FIXED_UUID, RESERVATION_CODE, FIXED_REQUEST_ID, mockExpiredOneReservationSeat(), 30000, LocalDateTime.now().minusMinutes(10), null);
    }

    public static Reservation mockOneSeatReservation(List<ReservationSeat> reservationSeats) {
        return Reservation.of(1L, FIXED_UUID, RESERVATION_CODE, FIXED_REQUEST_ID, reservationSeats, 30000, LocalDateTime.now().plusMinutes(10), null);
    }

    public static Reservation mockReservation(List<ReservationSeat> reservationSeats) {
        return Reservation.of(1L, FIXED_UUID, RESERVATION_CODE, FIXED_REQUEST_ID, reservationSeats, 30000, LocalDateTime.now().plusMinutes(10), null);
    }

    public static List<ReservationSeat> mockOneReservationSeat() {
        return List.of(ReservationSeat.of(1L,1L,1L,1L, ReservationSeatStatus.HOLD));
    }

    public static List<ReservationSeat> mockExpiredOneReservationSeat() {
        return List.of(ReservationSeat.of(1L,1L,1L,1L, ReservationSeatStatus.EXPIRED));
    }

    public static List<ReservationSeat> mockConfirmedOneReservationSeat() {
        return List.of(ReservationSeat.of(1L,1L,1L,1L, ReservationSeatStatus.CONFIRMED));
    }

    public static List<ReservationSeat> mockCanceledOneReservationSeat() {
        return List.of(ReservationSeat.of(1L,1L,1L,1L, ReservationSeatStatus.CANCELED));
    }

    public static List<ReservationSeat> mockReservationSeats() {
        return List.of(
                ReservationSeat.of(1L,1L,1L,1L,ReservationSeatStatus.HOLD),
                ReservationSeat.of(2L,1L,2L,1L,ReservationSeatStatus.HOLD),
                ReservationSeat.of(3L,1L,3L,1L,ReservationSeatStatus.HOLD),
                ReservationSeat.of(4L,1L,4L,1L,ReservationSeatStatus.HOLD)
        );
    }

    public static List<ReservationSeat> mockConfirmedReservationSeats() {
        return List.of(
                ReservationSeat.of(1L,1L,1L,1L,ReservationSeatStatus.CONFIRMED),
                ReservationSeat.of(2L,1L,2L,1L,ReservationSeatStatus.CONFIRMED),
                ReservationSeat.of(3L,1L,3L,1L,ReservationSeatStatus.CONFIRMED),
                ReservationSeat.of(4L,1L,4L,1L,ReservationSeatStatus.CONFIRMED)
        );
    }

    public static List<ReservationSeat> mockCanceledReservationSeats() {
        return List.of(
                ReservationSeat.of(1L,1L,1L,1L,ReservationSeatStatus.CANCELED),
                ReservationSeat.of(2L,1L,2L,1L,ReservationSeatStatus.CANCELED),
                ReservationSeat.of(3L,1L,3L,1L,ReservationSeatStatus.CANCELED),
                ReservationSeat.of(4L,1L,4L,1L,ReservationSeatStatus.CANCELED)
        );
    }

    public static List<ReservationSeat> mockReservationSeats(Long reservationId, ReservationSeatStatus status) {
        return List.of(
                ReservationSeat.of(1L, reservationId, 1L, 1L, status),
                ReservationSeat.of(2L, reservationId, 2L, 1L, status),
                ReservationSeat.of(3L, reservationId, 3L, 1L, status),
                ReservationSeat.of(4L, reservationId, 4L, 1L, status)
        );
    }

    public static List<VenueSeat> mockOneVenueSeat() {
        return List.of(VenueSeat.create(1L, "A", 1, 10000L));
    }

    public static List<VenueSeat> mockVenueSeats() {
        return List.of(
                VenueSeat.create(1L, "A", 1, 10000L),
                VenueSeat.create( 1L, "A", 2, 10000L),
                VenueSeat.create( 1L, "A", 3, 10000L)
        );
    }
}
