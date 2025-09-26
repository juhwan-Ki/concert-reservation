package com.gomdol.concert.venue.infra.persistence;

import com.gomdol.concert.venue.domain.model.Venue;
import com.gomdol.concert.venue.application.port.out.VenueRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class VenueRepositoryImpl implements VenueRepository {

    @Override
    public Optional<Venue> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Venue> findByConcertId(Long concertId) {
        return Optional.empty();
    }
}
