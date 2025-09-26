package com.gomdol.concert.reservation.application.port.in;

import com.gomdol.concert.reservation.domain.model.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "좌석 예약 응답")
public record ReservationResponse(
        @Schema(description = "예약 ID", example = "5001")
        Long reservationId,

        @Schema(description = "예약 코드", example = "reservation-asdas2135412-123412412")
        String reservationCode,

        @Schema(description = "만료 시간", example = "2025-09-11:12:10")
        LocalDateTime expiredAt

) {
        public static ReservationResponse fromDomain(Reservation reservation) {
                return new ReservationResponse(reservation.getId(), reservation.getReservationCode(), reservation.getExpiresAt());
        }
}
