package com.gomdol.concert.reservation.infra.persistence;

import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.infra.persistence.entity.ReservationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {

    private final ReservationJpaRepository reservationJpaRepository;

    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity entity = reservationJpaRepository.save(ReservationEntity.fromDomain(reservation));
        return ReservationEntity.toDomain(entity);
    }
}
