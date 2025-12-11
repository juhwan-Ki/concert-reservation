package com.gomdol.concert.show.infra.persistence.command;

import com.gomdol.concert.show.application.port.out.ShowRepository;
import com.gomdol.concert.show.domain.model.Show;
import com.gomdol.concert.show.infra.persistence.entity.ShowEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShowRepositoryImpl implements ShowRepository {

    private final ShowJpaRepository showJpaRepository;

    @Override
    public Optional<Show> findById(Long id) {
        return showJpaRepository.findByIdWithConcertAndVenue(id).map(ShowEntity::toDomain);
    }

    @Override
    public List<Show> findByConcertId(Long concertId) {
        return List.of();
    }

    @Override
    public List<Show> findByIds(List<Long> ids) {
        return showJpaRepository.findAllById(ids).stream().map(ShowEntity::toDomain).toList();
    }

    @Override
    public List<Long> findActiveShowIds(LocalDateTime now) {
        // 현재 시간 이후의 공연 ID 조회
        return showJpaRepository.findAll().stream()
                .filter(show -> show.getShowAt().isAfter(now))
                .map(ShowEntity::getId)
                .toList();
    }
}
