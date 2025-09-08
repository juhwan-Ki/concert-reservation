package com.gomdol.concert.concert.application;

import com.gomdol.concert.concert.application.service.ConcertService;
import com.gomdol.concert.concert.domain.repository.ConcertRepository;
import com.gomdol.concert.concert.fixture.ConcertFixture;
import com.gomdol.concert.concert.fixture.ShowFixture;
import com.gomdol.concert.concert.fixture.VenueFixture;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.show.domain.ShowStatus;
import com.gomdol.concert.show.domain.repository.ShowRepository;
import com.gomdol.concert.venue.domain.repository.VenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ConcertServiceTest {

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private ShowRepository showRepository;

    @InjectMocks
    private ConcertService concertService;

    /*
    * 콘서트 서비스 테스트
    * - 단건 조회
    * 1. 아이디에 해당하는 값이 존재하면 DTO로 매핑해 반환한다
    * 2. 아이디에 해당하는 값이 없으면 NotFoundException을 발생한다
    * 3. 일반 유저가 PUBLIC이 아닌 콘서트를 조회하면 ForbiddenException을 발생시킨다
    * 4. deleteAt 값이 not-null인 값이 조회되면 NotFoundException을 발생한다
    * 5. 현재 날짜가 시작일 이전이면 NotFoundException을 발생한다
    * 6. 현재 날짜가 종료일 이후면 NotFoundException을 발생한다
    * - 목록 조회
    * 1. 페이징 기본값을 적용해서 페이징이된 리스트를 반환한다
    * 2. 리스트 사이즈가 20을 넘는 값이 들어오면 20으로 고정된다
    * 3. 리스트 사이즈가 0보다 작으면 0으로 고정된다
    * 4. 결과가 비어 있으면 빈 리스트를 반환한다
    * TODO : 검색 조건은 따로 구현하지 않았는데 추후 구현 필요
    * */

    @Test
    @DisplayName("아이디에 해당하는 값이 존재하면 DTO로 매핑해 반환한다")
    public void 아이디에_해당하는_값이_존재하면_DTO로_매핑해_반환한다() throws Exception {
        // given
        Long id = 1L;
        when(concertRepository.findById(id)).thenReturn(Optional.of(ConcertFixture.create()));
        when(venueRepository.findByConcertId(id)).thenReturn(Optional.of(VenueFixture.create()));
        when(showRepository.findByConcertId(id)).thenReturn(ShowFixture.createList());
        // when
        ConcertDetailResponse response = concertService.getConcertById(id);
        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.title()).isNotBlank();
        assertThat(response.artist()).isNotBlank();
        assertThat(response.venueName()).isNotBlank();
        assertThat(response.ageRating()).isNotBlank();
        assertThat(response.posterUrl()).isNotBlank();
        assertThat(response.startAt()).isNotNull();
        assertThat(response.endAt()).isNotNull();
        assertThat(response.startAt()).isBeforeOrEqualTo(response.endAt());

        assertThat(response.showList())
                .isNotNull()
                .isNotEmpty()
                .allSatisfy(s -> {
                    assertThat(s.id()).isNotNull();
                    assertThat(s.showStatus()).isEqualTo(ShowStatus.ON_SALE);
                    assertThat(s.showAt()).isNotNull();
                });
    }
}
