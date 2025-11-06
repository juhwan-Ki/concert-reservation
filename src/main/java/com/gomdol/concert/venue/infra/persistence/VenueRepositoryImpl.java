package com.gomdol.concert.venue.infra.persistence;

import com.gomdol.concert.venue.domain.model.Venue;
import com.gomdol.concert.venue.application.port.out.VenueRepository;
import com.gomdol.concert.venue.infra.persistence.entity.VenueEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VenueRepositoryImpl implements VenueRepository {

    private final VenueJpaRepository jpaRepository;

    @Override
    public Optional<Venue> findById(Long id) {
        return jpaRepository.findById(id).map(VenueEntity::toDomain);
    }
}
