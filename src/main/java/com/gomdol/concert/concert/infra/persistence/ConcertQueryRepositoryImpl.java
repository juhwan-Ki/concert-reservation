package com.gomdol.concert.concert.infra.persistence;

import com.gomdol.concert.common.dto.PageResponse;
import com.gomdol.concert.concert.domain.Concert;
import com.gomdol.concert.concert.domain.ConcertStatus;
import com.gomdol.concert.concert.domain.repository.ConcertQueryRepository;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
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
    public Optional<ConcertDetailResponse> findPublicDetailById(Long id) {
        return concertJpaRepository.findPublicDetailById(id, ConcertStatus.PUBLIC)
                .map(ConcertDetailResponse::from);
    }

    @Override
    public PageResponse<ConcertResponse> findAllPublicAndKeyWord(Pageable pageable, String keyword) {
        Page<ConcertEntity> entityPage = concertJpaRepository.findAllPublicAndKeyWord(pageable, keyword, ConcertStatus.PUBLIC);
        return PageResponse.from(entityPage.map(ConcertResponse::from));
    }

    @Override
    public PageResponse<ConcertResponse> findAllPublic(Pageable pageable) {
        Page<ConcertEntity> entityPage = concertJpaRepository.findAllPublic(pageable);
        return PageResponse.from(entityPage.map(ConcertResponse::from));
    }
}
