package com.gomdol.concert.show.application.port.out;

import com.gomdol.concert.show.infra.persistence.query.ShowProjection;
import java.util.List;

public interface ShowQueryRepository {
    List<ShowProjection> findShowsByConcertId(Long concertId);
    boolean existsById(Long showId);
}
