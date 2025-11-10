package com.gomdol.concert.venue.application.port.out;

import com.gomdol.concert.venue.domain.model.Venue;

import java.util.Optional;

public interface VenueRepository {
    Optional<Venue> findById(Long id);
}
