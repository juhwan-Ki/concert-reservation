package com.gomdol.concert.concert.application.usecase;

import com.gomdol.concert.common.dto.PageResponse;
import com.gomdol.concert.common.util.PageableUtils;
import com.gomdol.concert.concert.application.port.in.ConcertQueryPort;
import com.gomdol.concert.concert.domain.policy.ConcertPolicies;
import com.gomdol.concert.concert.application.port.out.ConcertQueryRepository;
import com.gomdol.concert.concert.infra.persistence.query.ConcertDetailProjection;
import com.gomdol.concert.show.infra.persistence.query.ShowProjection;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import com.gomdol.concert.show.application.port.out.ShowQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

// 조회용 서비스
@Service
@RequiredArgsConstructor
public class ConcertQueryUseCase implements ConcertQueryPort {

    private final ConcertQueryRepository concertQueryRepository;
    private final ShowQueryRepository showQueryRepository;
    private final Clock clock;

    public ConcertDetailResponse getConcertById(Long id) {
        ConcertDetailProjection concert = concertQueryRepository.findPublicDetailById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트 입니다"));
        // 검증 로직
        ConcertPolicies.validateDeleted(concert.getDeletedAt());
        ConcertPolicies.validateInPeriod(concert.getStartAt(), concert.getEndAt(), LocalDate.now(clock));
        List<ShowProjection> shows = showQueryRepository.findShowsByConcertId(id);

        return ConcertDetailResponse.from(concert,shows);
    }

    public PageResponse<ConcertResponse> getConcertList(Pageable pageable, String keyword) {
        Pageable paging = PageableUtils.sanitize(pageable);
        if(keyword == null || keyword.isBlank())
            return concertQueryRepository.findAllPublic(paging);
        else
            return concertQueryRepository.findAllPublicAndKeyWord(paging, keyword);
    }

}
