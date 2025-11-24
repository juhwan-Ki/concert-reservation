package com.gomdol.concert.reservation.infra.persistence;

import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import com.gomdol.concert.reservation.infra.persistence.entity.ReservationSeatEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationSeatJpaRepository extends JpaRepository<ReservationSeatEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000") // 3초 타임아웃
    })
    @Query("""
    SELECT rs FROM ReservationSeatEntity rs
    WHERE rs.showId = :showId
      AND rs.seatId IN :seatIds
      AND rs.status IN :statuses
    ORDER BY rs.seatId
    """)
    List<ReservationSeatEntity> findByShowIdAndSeatIdsWithLock(
            @Param("showId") Long showId,
            @Param("seatIds") List<Long> seatIds,
            @Param("statuses") List<ReservationSeatStatus> statuses
    );
}
