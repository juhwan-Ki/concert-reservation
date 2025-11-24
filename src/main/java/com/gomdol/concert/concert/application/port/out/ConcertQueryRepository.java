package com.gomdol.concert.concert.application.port.out;

import com.gomdol.concert.common.presentation.dto.PageResponse;
import com.gomdol.concert.concert.infra.persistence.query.ConcertDetailProjection;
import com.gomdol.concert.show.infra.persistence.query.ShowProjection;
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
