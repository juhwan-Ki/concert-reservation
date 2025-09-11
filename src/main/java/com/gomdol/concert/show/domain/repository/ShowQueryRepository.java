package com.gomdol.concert.show.domain.repository;

import com.gomdol.concert.show.infra.query.projection.ShowProjection;
import java.util.List;

public interface ShowQueryRepository {
    List<ShowProjection> findShowsByConcertId(Long concertId);
}
