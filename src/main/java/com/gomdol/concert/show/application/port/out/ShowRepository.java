package com.gomdol.concert.show.application.port.out;

import com.gomdol.concert.show.domain.model.Show;

import java.util.List;
import java.util.Optional;

public interface ShowRepository {
    Optional<Show> findById(Long id);
    List<Show> findByConcertId(Long concertId);
}
