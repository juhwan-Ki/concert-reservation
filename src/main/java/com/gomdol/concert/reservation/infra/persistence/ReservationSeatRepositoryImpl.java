package com.gomdol.concert.reservation.infra.persistence;

import com.gomdol.concert.reservation.application.port.out.ReservationSeatRepository;
import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReservationSeatRepositoryImpl implements ReservationSeatRepository {

    private final ReservationSeatJpaRepository reservationSeatJpaRepository;

    @Override
    public boolean existsByShowIdAndSeatIdsAndStatus(Long showId, List<Long> seatIds, List<ReservationSeatStatus> statuses) {
        return !reservationSeatJpaRepository.findByShowIdAndSeatIdsWithLock(showId, seatIds, statuses).isEmpty();
    }
}
