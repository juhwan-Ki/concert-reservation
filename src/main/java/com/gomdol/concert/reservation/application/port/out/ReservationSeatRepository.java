package com.gomdol.concert.reservation.application.port.out;

import java.util.List;

public interface ReservationSeatRepository {
    boolean existsByShowIdAndIdIn (Long showId, List<Long> seatIds);
}
