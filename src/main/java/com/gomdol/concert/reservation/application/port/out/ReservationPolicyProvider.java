package com.gomdol.concert.reservation.application.port.out;

public interface ReservationPolicyProvider {
    int holdMinutes();              // 예약 홀드 유지 시간 (분)
    int maxSeatsPerReservation();   // 1회 예약 최대 좌석 수
    int maxRetryCount();                 // 재시도 횟수
    long retryDelayMillis();
}
