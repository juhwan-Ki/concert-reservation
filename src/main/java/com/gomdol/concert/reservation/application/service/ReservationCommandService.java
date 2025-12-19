package com.gomdol.concert.reservation.application.service;

import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reservation 도메인 Command 서비스
 * 개별 작업 단위
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationCommandService {

    private final ReservationRepository reservationRepository;

    /**
     * 좌석 확정
     */
    @Transactional
    public void confirmSeats(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다. ID: " + reservationId));

        reservation.confirmSeats();
        reservationRepository.save(reservation);

        log.info("좌석 확정 완료 - reservationId={}", reservationId);
    }

    /**
     * 좌석 취소 (보상 트랜잭션)
     */
    @Transactional
    public void cancelSeats(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다. ID: " + reservationId));

        reservation.cancelSeats();
        reservationRepository.save(reservation);

        log.info("좌석 취소 완료 (보상) - reservationId={}", reservationId);
    }
}
