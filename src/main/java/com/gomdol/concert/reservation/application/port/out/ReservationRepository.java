package com.gomdol.concert.reservation.application.port.out;

import com.gomdol.concert.reservation.domain.model.Reservation;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
}
