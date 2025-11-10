package com.gomdol.concert.show.infra.persistence.command;

import com.gomdol.concert.show.infra.persistence.entity.ShowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowJpaRepository extends JpaRepository<ShowEntity, Long> {
}