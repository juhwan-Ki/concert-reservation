package com.gomdol.concert.concert.domain.repository;

import com.gomdol.concert.concert.domain.Concert;

import java.util.Optional;

public interface ConcertRepository {
    Optional<Concert> findById(Long id);
}
