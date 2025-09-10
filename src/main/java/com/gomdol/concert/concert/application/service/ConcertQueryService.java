package com.gomdol.concert.concert.application.service;

import com.gomdol.concert.common.dto.PageResponse;
import com.gomdol.concert.common.util.PageableUtils;
import com.gomdol.concert.concert.domain.Concert;
import com.gomdol.concert.concert.domain.repository.ConcertQueryRepository;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import com.gomdol.concert.show.domain.Show;
import com.gomdol.concert.show.domain.repository.ShowRepository;
import com.gomdol.concert.venue.domain.Venue;
import com.gomdol.concert.venue.domain.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

// 조회용 서비스
@Service
@RequiredArgsConstructor
public class ConcertQueryService {

    // TODO: 추후 port 사용하여 infra에 직접 연결하지 않도록 변경
    private final ConcertQueryRepository concertQueryRepository;

    public ConcertDetailResponse getConcertById(Long id) {
        return concertQueryRepository.findPublicDetailById(id).orElseThrow(IllegalArgumentException::new);
    }

    public PageResponse<ConcertResponse> getConcertList(Pageable pageable, String keyword) {
        Pageable paging = PageableUtils.sanitize(pageable);
        if(keyword == null || keyword.isEmpty())
            return concertQueryRepository.findAllPublic(paging);
        else
            return concertQueryRepository.findAllPublicAndKeyWord(paging, keyword);
    }

}
