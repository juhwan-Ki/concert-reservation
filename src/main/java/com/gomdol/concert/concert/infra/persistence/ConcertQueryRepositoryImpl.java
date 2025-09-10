package com.gomdol.concert.concert.infra.persistence;

import com.gomdol.concert.concert.domain.Concert;
import com.gomdol.concert.concert.domain.ConcertStatus;
import com.gomdol.concert.concert.domain.repository.ConcertQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertQueryRepositoryImpl implements ConcertQueryRepository {

    private final ConcertQueryJpaRepository concertJpaRepository;

    @Override
    public Optional<Concert> findById(Long id) {
        Optional<ConcertEntity> concert = concertJpaRepository.findById(id);
        return concert.map(ConcertEntity::toDomain);
    }

    @Override
    public Page<Concert> findAllByStatus(ConcertStatus status, Pageable pageable) {
        Page<ConcertEntity> entityPage = concertJpaRepository.findAllByStatus(status, pageable);
        return entityPage.map(ConcertEntity::toDomain);
    }

    @Override
    public Page<Concert> findAllPublicAndKeyWord(Pageable pageable, String keyword) {
        Page<ConcertEntity> entityPage = concertJpaRepository.findAllPublicAndKeyWord(pageable, keyword);
        return entityPage.map(ConcertEntity::toDomain);
    }

    @Override
    public Page<Concert> findAllPublic(Pageable pageable) {
        Page<ConcertEntity> entityPage = concertJpaRepository.findAllPublic(pageable);
        return entityPage.map(ConcertEntity::toDomain);
    }
}
