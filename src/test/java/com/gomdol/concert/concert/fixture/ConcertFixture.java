package com.gomdol.concert.concert.fixture;

import com.gomdol.concert.common.dto.PageResponse;
import com.gomdol.concert.concert.domain.AgeRating;
import com.gomdol.concert.concert.domain.Concert;
import com.gomdol.concert.concert.domain.ConcertStatus;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;


public class ConcertFixture {

    public static Concert create() {
        return Concert.create(1L, "QWER 콘서트", "QWER", "QWER 단독 콘서트......",180, AgeRating.AGE_15,
                "https://asdasdasd1123easasdasd.jpg", "https://vkasjdkfljsdafbnalkdsjk.jpg",
                ConcertStatus.PUBLIC, LocalDate.of(2025,9,7),
                LocalDate.of(2025, 9, 10) );
    }

    public static Concert createConcertWithStatusNotPublic() {
        return Concert.create(10L, "QWER 콘서트", "QWER", "QWER 단독 콘서트......",200, AgeRating.AGE_15,
                "https://asdasdasd1123easasdasd.jpg", "https://vkasjdkfljsdafbnalkdsjk.jpg",
                ConcertStatus.PRIVATE, LocalDate.of(2025,9,7),
                LocalDate.of(2025, 9, 10) );
    }

    public static Page<Concert> createConcertPage(Pageable pageable) {
        List<Concert> content = createConcertList(3, 1L);
        return new PageImpl<>(content, pageable, content.size());
    }

    /** 개수만 지정 (page=0,size=20, id는 startId부터 증가) */
    public static Page<Concert> createConcertPage(int count, long startId) {
        Pageable pageable = PageRequest.of(0, 20);
        List<Concert> content = createConcertList(count, startId);
        return new PageImpl<>(content, pageable, content.size());
    }

    /** 페이지/사이즈/총개수까지 지정 (테스트 시 totalElements 제어용) */
    public static Page<Concert> createConcertPage(int count, long startId, int page, int size, long totalElements) {
        Pageable pageable = PageRequest.of(page, size);
        List<Concert> content = createConcertList(count, startId);
        return new PageImpl<>(content, pageable, totalElements);
    }

    /** 빈 페이지가 필요할 때 */
    public static Page<Concert> createEmpty(Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0);
    }

    private static List<Concert> createConcertList(int count, long startId) {
        return IntStream.range(0, count)
                .mapToObj(i -> sampleConcert(startId + i))
                .toList();
    }

    private static Concert sampleConcert(long id) {
        // 도메인 팩토리/생성자에 맞게 수정하세요.
        return Concert.create(
                id,
                "콘서트 " + id,
                "QWER",
                "샘플 설명",
                120,
                AgeRating.ALL,
                "https://example.com/thumb/" + id + ".jpg",
                "https://example.com/poster/" + id + ".jpg",
                ConcertStatus.PUBLIC,       // 예: PUBLIC/PRIVATE/DRAFT 등
                LocalDate.of(2025, 8, 12),
                LocalDate.of(2025, 8, 12)
        );
    }

    public static PageResponse<ConcertResponse> createConcertDtoList() {
        List<ConcertResponse> content = List.of(
                new ConcertResponse(
                        1L,
                        "보컬 전쟁 시즌2 “The War of Vocalists II”",
                        "인천 인스파이어 아레나",
                        "QWER",
                        ConcertStatus.PUBLIC,
                        LocalDate.of(2025, 8, 12),
                        LocalDate.of(2025, 8, 12),
                        "https://example.com/thumb/1.jpg"
                ),
                new ConcertResponse(
                        2L,
                        "락 페스티벌 2025",
                        "잠실실내체육관",
                        "ABC",
                        ConcertStatus.PUBLIC,
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 9, 1),
                        "https://example.com/thumb/2.jpg"
                ),
                new ConcertResponse(
                        3L,
                        "재즈 나이트",
                        "올림픽홀",
                        "XYZ",
                        ConcertStatus.PUBLIC,
                        LocalDate.of(2025, 10, 10),
                        LocalDate.of(2025, 10, 10),
                        "https://example.com/thumb/3.jpg"
                )
        );

        int page = 0;
        int size = 20;
        long total = content.size();
        int totalPages = 1;
        boolean first = true;
        boolean last = true;

        return new PageResponse<>(content, page, size, total, totalPages, first, last);
    }
}
