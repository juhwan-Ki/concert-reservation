package com.gomdol.concert.reservation.infra.persistence;

import com.gomdol.concert.reservation.infra.persistence.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {
    @Query("SELECT r FROM ReservationEntity r WHERE r.requestId = :requestId")
    Optional<ReservationEntity> findByRequestId(@Param("requestId") String requestId);
}
