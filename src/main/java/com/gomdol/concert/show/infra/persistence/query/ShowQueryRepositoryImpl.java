package com.gomdol.concert.show.infra.persistence.query;

import com.gomdol.concert.show.domain.model.ShowStatus;
import com.gomdol.concert.show.application.port.out.ShowQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ShowQueryRepositoryImpl implements ShowQueryRepository {

    private final ShowQueryJpaRepository showQueryJpaRepository;

    @Override
    public List<ShowProjection> findShowsByConcertId(Long concertId) {
        return showQueryJpaRepository.findByConcertIdAndStatus(concertId, ShowStatus.SCHEDULED);
    }

    @Override
    public boolean existsById(Long showId) {
        return showQueryJpaRepository.existsById(showId);
    }
}
