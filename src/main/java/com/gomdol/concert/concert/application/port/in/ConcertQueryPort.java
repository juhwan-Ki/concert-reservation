package com.gomdol.concert.concert.application.port.in;

import com.gomdol.concert.common.dto.PageResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import org.springframework.data.domain.Pageable;

public interface ConcertQueryPort {
    /**
     * 콘서트 상세 정보 조회
     * @param id 콘서트 ID
     * @return 콘서트 상세 정보 (공연 목록 포함)
     */
    ConcertDetailResponse getConcertById(Long id);

    /**
     * 콘서트 목록 조회 (검색 지원)
     * @param pageable 페이징 정보
     * @param keyword 검색 키워드 (nullable)
     * @return 페이징된 콘서트 목록
     */
    PageResponse<ConcertResponse> getConcertList(Pageable pageable, String keyword);
}
