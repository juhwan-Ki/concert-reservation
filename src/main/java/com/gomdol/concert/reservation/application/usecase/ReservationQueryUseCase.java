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
 * - 멱등성 체크를 위한 별도 트랜잭션 조회
 * - REQUIRES_NEW로 새 트랜잭션에서 실행하여 커밋된 데이터 조회 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationQueryUseCase {

    private final ReservationRepository reservationRepository;

    /**
     * requestId로 예약 조회 (멱등성 체크용)
     * 새로운 읽기 전용 트랜잭션에서 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Reservation> findByRequestId(String requestId) {
        return reservationRepository.findByRequestId(requestId);
    }
}