package com.gomdol.concert.concert.domain.repository;

import com.gomdol.concert.common.dto.PageResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ConcertQueryRepository {
    Optional<ConcertDetailResponse> findPublicDetailById(Long id);
    PageResponse<ConcertResponse> findAllPublicAndKeyWord(Pageable pageable, String keyword);
    PageResponse<ConcertResponse> findAllPublic(Pageable pageRequest);
}
