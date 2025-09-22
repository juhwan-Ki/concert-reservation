package com.gomdol.concert.reservation.application.port.in;

import java.util.List;

public record ReservationSeatCommand(
        String userId,
        Long showId,
        List<Long> seatIds
) {
}
