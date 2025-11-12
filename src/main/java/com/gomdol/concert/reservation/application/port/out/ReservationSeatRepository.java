package com.gomdol.concert.reservation.application.port.out;

import com.gomdol.concert.reservation.domain.ReservationSeatStatus;

import java.util.List;

public interface ReservationSeatRepository {
    boolean existsByShowIdAndSeatIdsAndStatus (Long showId, List<Long> seatIds, List<ReservationSeatStatus> statuses);
}
