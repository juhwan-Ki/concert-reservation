package com.gomdol.concert.venue.infra.persistence;

import com.gomdol.concert.venue.infra.persistence.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueJpaRepository extends JpaRepository<VenueEntity, Long> {
}