package com.gomdol.concert.concert.fixture;

import com.gomdol.concert.common.presentation.dto.PageResponse;
import com.gomdol.concert.concert.domain.model.AgeRating;
import com.gomdol.concert.concert.domain.model.Concert;
import com.gomdol.concert.concert.domain.model.ConcertStatus;
import com.gomdol.concert.concert.infra.persistence.query.ConcertDetailProjection;
import com.gomdol.concert.show.infra.persistence.query.ShowProjection;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import com.gomdol.concert.show.domain.model.ShowStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ConcertFixture {

    public static ConcertDetailProjection create() {
        return ConcertDetailStub.builder()
                .id(1L)
                .title("QWER 콘서트")
                .venueName("인천 인스파이어 아레나")
                .artist("QWER")
                .ageRating(AgeRating.AGE_12.getDesc())
                .runningTime(150)
                .description("QWER 단독 콘서트......")
                .posterUrl("https://vkasjdkfljsdafbnalkdsjk.jpg")
                .startAt(LocalDate.of(2025,8,12))
                .endAt(LocalDate.of(2025,8,20))
                .build();
    }

    public static ConcertDetailProjection beforeConcertStart() {
        return ConcertDetailStub.builder()
                .id(1L)
                .title("QWER 콘서트")
                .venueName("인천 인스파이어 아레나")
                .artist("QWER")
                .ageRating(AgeRating.AGE_12.getDesc())
                .runningTime(150)
                .description("QWER 단독 콘서트......")
                .posterUrl("https://vkasjdkfljsdafbnalkdsjk.jpg")
                .startAt(LocalDate.of(2025,8,16))
                .endAt(LocalDate.of(2025,8,20))
                .build();
    }

    public static ConcertDetailProjection afterConcertEnd() {
        return ConcertDetailStub.builder()
                .id(1L)
                .title("QWER 콘서트")
                .venueName("인천 인스파이어 아레나")
                .artist("QWER")
                .ageRating(AgeRating.AGE_12.getDesc())
                .runningTime(150)
                .description("QWER 단독 콘서트......")
                .posterUrl("https://vkasjdkfljsdafbnalkdsjk.jpg")
                .startAt(LocalDate.of(2025,8,12))
                .endAt(LocalDate.of(2025,8,14))
                .build();
    }



    public static ConcertDetailProjection deleteConcert() {
        return ConcertDetailStub.builder()
                .id(1L)
                .title("QWER 콘서트")
                .venueName("인천 인스파이어 아레나")
                .artist("QWER")
                .ageRating(AgeRating.AGE_12.getDesc())
                .runningTime(150)
                .description("QWER 단독 콘서트......")
                .posterUrl("https://vkasjdkfljsdafbnalkdsjk.jpg")
                .startAt(LocalDate.of(2025,8,12))
                .endAt(LocalDate.of(2025,8,20))
                .deletedAt(LocalDateTime.of(2025,8,21,11,0))
                .build();
    }

    public static List<ShowProjection> createShows() {
        List<ShowProjection> shows = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            shows.add(ShowStub.builder()
                    .id((long)i)
                    .status(ShowStatus.ON_SALE.getDesc())
                    .showAt(LocalDateTime.of(2025,8,i,17,0))
                    .build());
        }
        return shows;
    }

    public static Concert createConcertWithStatusNotPublic() {
        return Concert.create(10L, "QWER 콘서트", "QWER", "QWER 단독 콘서트......",200, AgeRating.AGE_15,
                "https://asdasdasd1123easasdasd.jpg", "https://vkasjdkfljsdafbnalkdsjk.jpg",
                ConcertStatus.PRIVATE, LocalDate.of(2025,9,7),
                LocalDate.of(2025, 9, 10) );
    }

    /**
     * 기본 샘플 3개짜리 PageResponse
     */
    public static PageResponse<ConcertResponse> defaultConcertPage(Pageable pageable) {
        return createConcertDtoList(3, pageable);
    }

    /**
     * 원하는 개수만큼 ConcertResponse 생성
     */
    public static PageResponse<ConcertResponse> createConcertDtoList(int totalCount, Pageable pageable) {
        List<ConcertResponse> all = new ArrayList<>();
        for (int i = 1; i <= totalCount; i++) {
            all.add(new ConcertResponse(
                    (long) i,
                    "콘서트 제목 " + i,
                    "공연장 " + i,
                    "아티스트 " + i,
                    ConcertStatus.PUBLIC,
                    LocalDate.of(2025, 8, 1).plusDays(i),
                    LocalDate.of(2025, 8, 1).plusDays(i),
                    "https://example.com/thumb/" + i + ".jpg"
            ));
        }

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalCount);

        List<ConcertResponse> content = (fromIndex >= totalCount) ?
                List.of() : all.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) totalCount / size);
        boolean first = page == 0;
        boolean last = page >= totalPages - 1;

        return new PageResponse<>(content, page, size, totalCount, totalPages, first, last);
    }

    public static PageResponse<ConcertResponse> createEmpty(Pageable pageable) {
        return new PageResponse<>(
                List.of(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                0,
                0,
                pageable.getPageNumber() == 0,
                true
        );
    }
}
