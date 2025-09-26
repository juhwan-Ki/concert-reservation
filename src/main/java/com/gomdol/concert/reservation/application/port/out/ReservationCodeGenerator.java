package com.gomdol.concert.reservation.application.port.out;

import java.util.UUID;

public interface ReservationCodeGenerator {
    String newReservationCode();
}
