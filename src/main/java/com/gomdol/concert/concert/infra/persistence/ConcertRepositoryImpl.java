package com.gomdol.concert.concert.infra.persistence;

import com.gomdol.concert.concert.domain.Concert;
import com.gomdol.concert.concert.domain.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertRepositoryImpl implements ConcertRepository {

    private final ConcertJpaRepository concertJpaRepository;

    @Override
    public Optional<Concert> findById(Long id) {
        Optional<ConcertEntity> concert = concertJpaRepository.findById(id);
        return concert.map(ConcertEntity::toDomain);
    }
}
