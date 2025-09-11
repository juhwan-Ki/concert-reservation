package com.gomdol.concert.concert.domain.repository;

import com.gomdol.concert.common.dto.PageResponse;
import com.gomdol.concert.concert.infra.query.projection.ConcertDetailProjection;
import com.gomdol.concert.show.infra.query.projection.ShowProjection;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ConcertQueryRepository {
    Optional<ConcertDetailProjection> findPublicDetailById(Long id);
    List<ShowProjection> findShowsByConcertId(Long id);
    PageResponse<ConcertResponse> findAllPublicAndKeyWord(Pageable pageable, String keyword);
    PageResponse<ConcertResponse> findAllPublic(Pageable pageRequest);
}
