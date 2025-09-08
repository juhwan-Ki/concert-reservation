package com.gomdol.concert.venue.domain.repository;

import com.gomdol.concert.venue.domain.Venue;

import java.util.Optional;

public interface VenueRepository {
    Optional<Venue> findById(Long id);
    Optional<Venue> findByConcertId(Long concertId);
}
