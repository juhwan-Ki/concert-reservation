package com.gomdol.concert.show.infra.persistence.command;

import com.gomdol.concert.show.infra.persistence.entity.ShowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShowJpaRepository extends JpaRepository<ShowEntity, Long> {

    @Query("SELECT s FROM ShowEntity s " +
            "JOIN FETCH s.concert c " +
            "JOIN FETCH c.venue " +
            "WHERE s.id = :id")
    Optional<ShowEntity> findByIdWithConcertAndVenue(@Param("id") Long id);
}