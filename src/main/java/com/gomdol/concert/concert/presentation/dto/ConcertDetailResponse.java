package com.gomdol.concert.concert.presentation.dto;

import com.gomdol.concert.concert.infra.persistence.query.ConcertDetailProjection;
import com.gomdol.concert.show.infra.persistence.query.ShowProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ConcertDetailResponse(
        @Schema(example = "1", description = "콘서트 id")
        Long id,

        @Schema(example = "보컬 전쟁 시즌2 “The War of Vocalists II”", description = "콘서트 명")
        String title,

        @Schema(example = "인천 인스파이어 아레나", description = "공연장 명")
        String venueName,

        @Schema(example = "QWER", description = "아티스트 명")
        String artist,

        @Schema(example = "만 7세이상", description = "관람연령")
        String ageRating,

        @Schema(example = "150분", description = "공연시간")
        String runningTime,

        @Schema(example = "셰익스피어의 대표작을 현대적으로 재해석한...", description = "공연정보")
        String description,

        @Schema(description="포스터 이미지 url", example = "https://imga-asdasdasc.com?asdasdas")
        String posterUrl,

        @Schema(example = "2025-08-12", description = "시작일")
        LocalDate startAt,

        @Schema(example = "2025-08-12", description = "종료일")
        LocalDate endAt,

        @Schema(description = "공연 리스트")
        List<ShowResponse> showList,

        @Schema(description = "삭제일자")
        LocalDateTime deletedAt
){
        public static ConcertDetailResponse from( ConcertDetailProjection c,  List<ShowProjection> shows) {
                List<ShowResponse> showList = shows.stream()
                        .map(ShowResponse::from)
                        .toList();

                return new ConcertDetailResponse(
                        c.getId(),
                        c.getTitle(),
                        c.getVenueName(),
                        c.getArtist(),
                        c.getAgeRating(),
                        minutesToText(c.getRunningTime()),
                        c.getDescription(),
                        c.getPosterUrl(),
                        c.getStartAt(),
                        c.getEndAt(),
                        showList,
                        c.getDeletedAt()
                );
        }

        private static String minutesToText(Integer minutes) {
                return (minutes == null) ? null : minutes + "분";
        }
}
