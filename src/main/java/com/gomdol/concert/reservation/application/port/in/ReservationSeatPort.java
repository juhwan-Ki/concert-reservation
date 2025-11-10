package com.gomdol.concert.reservation.application.port.in;

import java.util.List;

public interface ReservationSeatPort {
    ReservationResponse reservationSeat(ReservationSeatCommand command);
    record ReservationSeatCommand(String userId, String requestId, Long showId, List<Long> seatIds) {}
}
