package com.gomdol.concert.venue.infra.persistence;

import com.gomdol.concert.venue.infra.persistence.entity.VenueSeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueSeatJpaRepository extends JpaRepository<VenueSeatEntity, Long> {
}
