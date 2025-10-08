package com.gomdol.concert.reservation.infra.persistence;

import com.gomdol.concert.reservation.infra.persistence.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {
    Optional<ReservationEntity> findByRequestId(String requestId);
}
