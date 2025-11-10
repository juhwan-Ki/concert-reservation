package com.gomdol.concert.concert.infra.persistence.command;

import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertJpaRepository extends JpaRepository<ConcertEntity, Long> {
}