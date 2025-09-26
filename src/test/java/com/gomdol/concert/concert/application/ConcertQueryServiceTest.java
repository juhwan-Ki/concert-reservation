package com.gomdol.concert.concert.application;

import com.gomdol.concert.common.dto.PageResponse;
import com.gomdol.concert.concert.application.usecase.ConcertQueryService;
import com.gomdol.concert.concert.application.port.out.ConcertQueryRepository;
import com.gomdol.concert.concert.fixture.ConcertFixture;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import com.gomdol.concert.show.domain.ShowStatus;
import com.gomdol.concert.show.domain.repository.ShowQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ConcertQueryServiceTest {

    @Mock
    private ConcertQueryRepository concertQueryRepository;

    @Mock
    private ShowQueryRepository showQueryRepository;

    @Spy
    private Clock clock = Clock.fixed(
            LocalDate.of(2025, 8, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    @InjectMocks
    private ConcertQueryService concertQueryService;

    /*
    * 콘서트 서비스 테스트
    * - 단건 조회
    * 1. 아이디에 해당하는 값이 존재하면 DTO로 매핑해 반환한다
    * 2. 아이디에 해당하는 값이 없으면 IllegalArgumentException을 발생한다
    * 3. 일반 유저가 PUBLIC이 아닌 콘서트를 조회하면 ForbiddenException을 발생시킨다
    * 4. deleteAt 값이 not-null인 값이 조회되면 IllegalArgumentException을 발생한다
    * 5. 현재 날짜가 시작일 이전이면 IllegalArgumentException을 발생한다
    * 6. 현재 날짜가 종료일 이후면 IllegalArgumentException을 발생한다
    * - 목록 조회
    * 1. 페이징 기본값을 적용해서 페이징이된 리스트를 반환한다
    * 2. 리스트 사이즈가 20을 넘는 값이 들어오면 20으로 고정된다
    * 3. 마지막 페이지 요청시 빈 결과 반환된다
    * 4. 키워드 존재에 따른 메소드 호출 테스트
    * TODO : 검색 조건은 따로 구현하지 않았는데 추후 구현 필요
    * */

    @Test
    @DisplayName("아이디에 해당하는 값이 존재하면 DTO로 매핑해 반환한다")
    public void 아이디에_해당하는_값이_존재하면_DTO로_매핑해_반환한다() throws Exception {
        // given
        Long id = 1L;
        when(concertQueryRepository.findPublicDetailById(id)).thenReturn(Optional.of(ConcertFixture.create()));
        when(showQueryRepository.findShowsByConcertId(id)).thenReturn(ConcertFixture.createShows());
        // when
        ConcertDetailResponse response = concertQueryService.getConcertById(id);
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
                    assertThat(s.showStatus()).isEqualTo(ShowStatus.ON_SALE.getDesc());
                    assertThat(s.showAt()).isNotNull();
                });
    }

    @Test
    @DisplayName("아이디에 해당하는 값이 없으면 IllegalArgumentException 예외를 던진다")
    public void 아이디에_해당하는_값이_없으면_IllegalArgumentException_예외를_던진다() throws Exception {
        // given
        Long id = 999L;
        when(concertQueryRepository.findPublicDetailById(id)).thenReturn(Optional.empty());
        // when & then
        assertThatThrownBy(() -> concertQueryService.getConcertById(id)).isInstanceOf(IllegalArgumentException.class);
        verify(concertQueryRepository).findPublicDetailById(id);
    }

    @Test
    @DisplayName("deleteAt이 null이 아닌 값을 조회하면 IllegalArgumentException 예외를 던진다")
    public void deleteAt이_null이_아닌_값을_조회하면_IllegalArgumentException_예외를_던진다() throws Exception {
        // given
        Long id = 1L;
        when(concertQueryRepository.findPublicDetailById(id)).thenReturn(Optional.of(ConcertFixture.deleteConcert()));
        // when & then
        assertThatThrownBy(() -> concertQueryService.getConcertById(id)).isInstanceOf(IllegalArgumentException.class);
        verify(concertQueryRepository).findPublicDetailById(id);
    }

    @Test
    @DisplayName("현재 날짜가 시작일 이전이면 IllegalArgumentException 예외를 던진다")
    public void 현재_날짜가_시작일_이전이면_IllegalArgumentException_예외를_던진다() throws Exception {
        // given
        Long id = 1L;
        when(concertQueryRepository.findPublicDetailById(id)).thenReturn(Optional.of(ConcertFixture.beforeConcertStart()));
        // when & then
        assertThatThrownBy(() -> concertQueryService.getConcertById(id)).isInstanceOf(IllegalArgumentException.class);
        verify(concertQueryRepository).findPublicDetailById(id);
    }

    @Test
    @DisplayName("현재 날짜가 종료일 이후면 IllegalArgumentException 예외를 던진다")
    public void 현재_날짜가_종료일_이후면_IllegalArgumentException_예외를_던진다() throws Exception {
        // given
        Long id = 1L;
        when(concertQueryRepository.findPublicDetailById(id)).thenReturn(Optional.of(ConcertFixture.afterConcertEnd()));
        // when & then
        assertThatThrownBy(() -> concertQueryService.getConcertById(id)).isInstanceOf(IllegalArgumentException.class);
        verify(concertQueryRepository).findPublicDetailById(id);
    }

    @Test
    @DisplayName("페이징 기본값을 적용해서 페이징된 리스트를 반환한다")
    public void 페이징_기본값을_적용해서_페이징된_리스트를_반환한다() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        when(concertQueryRepository.findAllPublic(any(Pageable.class)))
                .thenReturn(ConcertFixture.defaultConcertPage(pageable));

        // when
        PageResponse<ConcertResponse> result = concertQueryService.getConcertList(PageRequest.of(0, 20), "");

        // then
        assertThat(result.content()).hasSize(3);
        verify(concertQueryRepository).findAllPublic(pageable);
    }

    @Test
    @DisplayName("리스트 사이즈가 20을 넘으면 20으로 고정된다")
    public void 리스트_사이즈가_20을_넘으면_20으로_고정된다() {
        when(concertQueryRepository.findAllPublic(any(Pageable.class)))
                .thenReturn(ConcertFixture.createEmpty(PageRequest.of(0, 20)));

        // when
        concertQueryService.getConcertList(PageRequest.of(0, 50), "");

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(concertQueryRepository).findAllPublic(captor.capture());
        Pageable passed = captor.getValue();

        assertThat(passed.getPageNumber()).isEqualTo(0);
        assertThat(passed.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("마지막 페이지 요청시 빈 결과 반환된다")
    void 마지막_페이지_요청시_빈_결과_반환된다() {
        // given
        Pageable pageRequest = PageRequest.of(999, 20);
        when(concertQueryRepository.findAllPublic(any(Pageable.class)))
                .thenReturn(ConcertFixture.createEmpty(pageRequest));

        // when
        PageResponse<ConcertResponse> result = concertQueryService.getConcertList(pageRequest, "");

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("키워드가 존재하면 findAllPublicAndKeyWord 메소드를 호출한다")
    void 키워드가_존재하면_findAllPublicAndKeyWord_메소드를_호출한다() {
        // when & then
        concertQueryService.getConcertList(PageRequest.of(0, 20), "keyword");
        verify(concertQueryRepository).findAllPublicAndKeyWord(PageRequest.of(0, 20), "keyword");
    }

    @ParameterizedTest
    @DisplayName("키워드가 비어 있으면 findAllPublic 메소드를 호출한다")
    @ValueSource(strings = {"", "   "})
    public void 키워드가_비어_있으면_findAllPublic_메소드를_호출한다(String keyword) {
        // when & then
        concertQueryService.getConcertList(PageRequest.of(0, 20), keyword);
        verify(concertQueryRepository).findAllPublic(PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("키워드가 null이면 findAllPublic 메소드를 호출한다")
    public void 키워드가_null이면_findAllPublic_메소드를_호출한다() {
        // when & then
        concertQueryService.getConcertList(PageRequest.of(0, 20), null);
        verify(concertQueryRepository).findAllPublic(PageRequest.of(0, 20));
    }

    /*@Test
    @DisplayName("상태가 PRIVATE인 콘서트는 제외가 된다")
    public void 상태가_PRIVATE인_콘서트는_제외가_된다() {
        // given
        Pageable pageRequest = PageRequest.of(0, 20);
        when(concertQueryRepository.findAllPublicAndKeyWord(any(Pageable.class), anyString()))
                .thenReturn(ConcertFixture.defaultConcertPage(pageRequest));

        // when
        PageResponse<ConcertResponse> result = concertQueryService.getConcertList(pageRequest,null);

        // then
        assertThat(result.content()).hasSize(3);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.content()).extracting(ConcertResponse::status).containsOnly(ConcertStatus.PUBLIC);

        verify(concertQueryService).getConcertList(pageRequest,null);
        verify(concertQueryRepository).findAllPublicAndKeyWord(pageRequest, null);
    }*/
}
