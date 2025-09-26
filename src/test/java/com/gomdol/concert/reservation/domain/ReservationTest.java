package com.gomdol.concert.reservation.domain;

import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;

public class ReservationTest {

    private static final String FIXED_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String RESERVATION_CODE = "reservationCode";
    private static final String FIXED_REQUEST_ID = "524910ab692b43c5b97ebadf176416cb7bf06da44f974b3bad33aca0778cebf7";

    @Test
    public void 좌석_상태가_HOLD인_신규_예약을_생성한다 () throws Exception {
        // given
        List<ReservationSeat> seats = mockReservationSeats(null, ReservationSeatStatus.HOLD);
        // when
        Reservation reservation = Reservation.create(FIXED_UUID, RESERVATION_CODE, FIXED_REQUEST_ID, seats, 40000L);
        // then
        assertThat(reservation.getId()).isNull();
        assertThat(reservation.getReservationCode()).isEqualTo(RESERVATION_CODE);
        assertThat(reservation.getReservationSeats().size()).isEqualTo(4);
        assertThat(reservation.getExpiresAt()).isNotNull();
        assertThat(reservation.getConfirmedAt()).isNull();
        assertThat(reservation.getReservationSeats().get(0).getStatus()).isEqualTo(ReservationSeatStatus.HOLD);
    }

    @Test
    public void 좌석_상태가_HOLD인_예약을_확정처리한다() throws Exception {
        // given
        Reservation reservation = mockHoldReservation();
        // when
        reservation.confirmSeats();
        // then
        assertThat(reservation.getId()).isEqualTo(1L);
        assertThat(reservation.getReservationCode()).isEqualTo(RESERVATION_CODE);
        assertThat(reservation.getReservationSeats().size()).isEqualTo(4);
        assertThat(reservation.getExpiresAt()).isNull();
        assertThat(reservation.getConfirmedAt()).isNotNull();
        assertThat(reservation.allSeatsAreConfirmed()).isTrue();
    }

    @Test
    void 좌석이_없으면_에러를_발생시킨다() {
        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                Reservation.create(FIXED_UUID, RESERVATION_CODE, FIXED_REQUEST_ID, List.of(), 40000L)
        );
    }

    @Test
    void 만료된_예약은_확정에_실패한다() {
        // given
        Reservation reservation = mockExpiredReservation();
        // when && then
        assertThatThrownBy(reservation::confirmSeats).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("만료");
    }

    @Test
    void 좌석_상태가_HOLD가_아니면_예약_확정에_실패한다() {
        // given
        Reservation reservation = mockCancelReservation();
        // when && then
        assertThatThrownBy(reservation::confirmSeats).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HOLD 상태");
    }

    @Test
    public void 좌석_상태가_HOLD인_예약을_취소처리한다() throws Exception {
        // given
        Reservation reservation = mockHoldReservation();
        // when
        reservation.cancelSeats();
        // then
        assertThat(reservation.getId()).isEqualTo(1L);
        assertThat(reservation.getReservationCode()).isEqualTo(RESERVATION_CODE);
        assertThat(reservation.getReservationSeats().size()).isEqualTo(4);
        assertThat(reservation.getExpiresAt()).isNull();
        assertThat(reservation.getConfirmedAt()).isNotNull();
        assertThat(reservation.allSeatsAreCancel()).isTrue();
    }

    @Test
    public void 좌석_상태가_CANCELED인_예약을_취소처리하면_에러를_발생시킨다() throws Exception {
        // given
        Reservation reservation = mockCancelReservation();
        // when && then
        assertThatThrownBy(reservation::cancelSeats).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("취소");
    }

    @Test
    void HOLD_시간이_지나면_만료된_상태이다() {
        // Given
        Reservation reservation = mockExpiredReservation();
        // When & Then
        assertThat(reservation.isExpired()).isTrue();
    }

    @Test
    void HOLD_시간이_지나지_않으면_만료되지_않은_상태이다() {
        // Given
        Reservation reservation = mockHoldReservation();

        // When & Then
        assertThat(reservation.isExpired()).isFalse();
    }

    private Reservation mockHoldReservation() {
        return Reservation.of(1L, FIXED_UUID, RESERVATION_CODE, FIXED_REQUEST_ID, mockReservationSeats(1L, ReservationSeatStatus.HOLD), 40000L, LocalDateTime.now().plusMinutes(10), null);
    }

    private Reservation mockConfirmedReservation() {
        return Reservation.of(1L, FIXED_UUID, RESERVATION_CODE, FIXED_REQUEST_ID, mockReservationSeats(1L, ReservationSeatStatus.CONFIRMED), 40000L, LocalDateTime.now().plusMinutes(10), null);
    }

    private Reservation mockExpiredReservation() {
        return Reservation.of(1L, FIXED_UUID, RESERVATION_CODE, FIXED_REQUEST_ID, mockReservationSeats(1L, ReservationSeatStatus.EXPIRED), 40000L, LocalDateTime.now().minusMinutes(1), null);
    }

    private Reservation mockCancelReservation() {
        return Reservation.of(1L, FIXED_UUID, RESERVATION_CODE, FIXED_REQUEST_ID, mockReservationSeats(1L, ReservationSeatStatus.CANCELED), 40000L, LocalDateTime.now().plusMinutes(1), null);
    }

    private List<ReservationSeat> mockReservationSeats(Long reservationId, ReservationSeatStatus status) {
        return List.of(
                ReservationSeat.of(1L, reservationId, 1L, 1L, status),
                ReservationSeat.of(2L, reservationId, 2L, 1L, status),
                ReservationSeat.of(3L, reservationId, 3L, 1L, status),
                ReservationSeat.of(4L, reservationId, 4L, 1L, status)
        );
    }
}
