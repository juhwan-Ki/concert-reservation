package com.gomdol.concert.reservation.infra.persistence;

import com.gomdol.concert.reservation.infra.persistence.entity.ReservationSeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatJpaRepository extends JpaRepository<ReservationSeatEntity, Long> {
}
