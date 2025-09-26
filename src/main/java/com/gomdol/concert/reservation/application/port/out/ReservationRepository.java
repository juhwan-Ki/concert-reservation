package com.gomdol.concert.reservation.application.port.out;

import com.gomdol.concert.reservation.domain.model.Reservation;

import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findByRequestId(String requestId);
}
