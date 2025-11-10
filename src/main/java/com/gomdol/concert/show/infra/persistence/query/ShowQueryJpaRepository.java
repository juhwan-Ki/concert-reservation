package com.gomdol.concert.show.infra.persistence.query;

import com.gomdol.concert.show.domain.model.ShowStatus;
import com.gomdol.concert.show.infra.persistence.entity.ShowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowQueryJpaRepository extends JpaRepository<ShowEntity, Long> {

    List<ShowProjection> findByConcertIdAndStatus(Long concertId, ShowStatus status);
}
