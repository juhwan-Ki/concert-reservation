package com.gomdol.concert.concert.infra.persistence.query;

import com.gomdol.concert.common.presentation.dto.PageResponse;
import com.gomdol.concert.concert.domain.model.ConcertStatus;
import com.gomdol.concert.concert.application.port.out.ConcertQueryRepository;
import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import com.gomdol.concert.show.infra.persistence.query.ShowProjection;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertQueryRepositoryImpl implements ConcertQueryRepository {

    private final ConcertQueryJpaRepository concertJpaRepository;

    @Override
    public Optional<ConcertDetailProjection> findPublicDetailById(Long id) {
        return concertJpaRepository.findByIdAndStatus(id, ConcertStatus.PUBLIC);
    }

    @Override
    public List<ShowProjection> findShowsByConcertId(Long id) {
        return concertJpaRepository.findShowsByConcertId(id);
    }

    @Override
    public PageResponse<ConcertResponse> findAllPublicAndKeyWord(Pageable pageable, String keyword) {
        Page<ConcertEntity> entityPage = concertJpaRepository.findAllByStatusAndKeyWord(pageable, keyword, ConcertStatus.PUBLIC);
        return PageResponse.from(entityPage.map(ConcertResponse::from));
    }

    @Override
    public PageResponse<ConcertResponse> findAllPublic(Pageable pageable) {
        Page<ConcertEntity> entityPage = concertJpaRepository.findAllByStatus(pageable, ConcertStatus.PUBLIC);
        return PageResponse.from(entityPage.map(ConcertResponse::from));
    }
}
