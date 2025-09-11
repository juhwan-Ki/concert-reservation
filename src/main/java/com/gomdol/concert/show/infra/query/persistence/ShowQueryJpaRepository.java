package com.gomdol.concert.show.infra.query.persistence;

import com.gomdol.concert.show.domain.ShowStatus;
import com.gomdol.concert.show.infra.command.persistence.ShowEntity;
import com.gomdol.concert.show.infra.query.projection.ShowProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowQueryJpaRepository extends JpaRepository<ShowEntity, Long> {

    List<ShowProjection> findByConcertIdAndStatus(Long concertId, ShowStatus status);
}
