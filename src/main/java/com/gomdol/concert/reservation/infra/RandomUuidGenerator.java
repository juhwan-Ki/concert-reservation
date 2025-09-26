package com.gomdol.concert.reservation.infra;

import com.gomdol.concert.reservation.application.port.out.ReservationCodeGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RandomUuidGenerator implements ReservationCodeGenerator {
    private static final String PREFIX = "reservation-";

    @Override
    public String newReservationCode() {
        return PREFIX + UUID.randomUUID();
    }
}
