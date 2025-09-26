package com.gomdol.concert.show.domain.repository;

import com.gomdol.concert.show.domain.Show;

import java.util.List;
import java.util.Optional;

public interface ShowRepository {
    Optional<Show> findById(Long id);
    List<Show> findByConcertId(Long concertId);
}
