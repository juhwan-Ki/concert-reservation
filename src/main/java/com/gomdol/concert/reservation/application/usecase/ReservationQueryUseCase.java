package com.gomdol.concert.reservation.application.usecase;

import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 예약 조회 전용 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationQueryUseCase {

    private final ReservationRepository reservationRepository;

}