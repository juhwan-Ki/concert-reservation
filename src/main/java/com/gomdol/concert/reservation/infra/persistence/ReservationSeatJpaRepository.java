package com.gomdol.concert.reservation.infra.persistence;

import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import com.gomdol.concert.reservation.infra.persistence.entity.ReservationSeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReservationSeatJpaRepository extends JpaRepository<ReservationSeatEntity, Long> {
    @Query("""
    select case when count(rs) > 0 then true else false end
    from ReservationSeatEntity rs
    where rs.showId = :showId
      and rs.id in :ids
      and rs.status in :statuses
    """)
    boolean existsByShowIdAndIdInAndStatus(Long showId, List<Long> ids, List<ReservationSeatStatus> statuses);
}
