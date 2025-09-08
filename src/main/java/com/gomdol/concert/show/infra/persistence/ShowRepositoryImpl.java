package com.gomdol.concert.show.infra.persistence;

import com.gomdol.concert.show.domain.Show;
import com.gomdol.concert.show.domain.repository.ShowRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ShowRepositoryImpl implements ShowRepository {

    @Override
    public Optional<Show> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public List<Show> findByConcertId(Long concertId) {
        return List.of();
    }
}
