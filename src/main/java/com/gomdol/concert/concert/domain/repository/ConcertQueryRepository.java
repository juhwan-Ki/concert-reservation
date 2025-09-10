package com.gomdol.concert.concert.domain.repository;

import com.gomdol.concert.concert.domain.Concert;
import com.gomdol.concert.concert.domain.ConcertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ConcertQueryRepository {
    Optional<Concert> findById(Long id);
    Page<Concert> findAllByStatus(ConcertStatus status, Pageable pageable);
    Page<Concert> findAllPublicAndKeyWord(Pageable pageable, String keyword);
    Page<Concert> findAllPublic(Pageable pageRequest);
}
