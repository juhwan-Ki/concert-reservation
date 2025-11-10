package com.gomdol.concert.concert.presentation.dto;

import com.gomdol.concert.concert.domain.model.ConcertStatus;
import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ConcertResponse(
        @Schema(example = "1", description = "콘서트 id")
        Long id,

        @Schema(example = "보컬 전쟁 시즌2 “The War of Vocalists II”", description = "콘서트 명")
        String title,

        @Schema(example = "인천 인스파이어 아레나", description = "공연장 명")
        String venueName,

        @Schema(example = "QWER", description = "아티스트 명")
        String artist,

        @Schema(example = "공개", description = "상태")
        ConcertStatus status,

        @Schema(example = "2025-08-12", description = "시작일")
        LocalDate startAt,

        @Schema(example = "2025-08-12", description = "종료일")
        LocalDate endAt,

        @Schema(description="섬네일 이미지 url", example = "https://imga-asdasdasc.com?asdasdas")
        @NotBlank String thumbnailUrl
) {
        public static ConcertResponse from(ConcertEntity concert) {
           return ConcertResponse.builder()
                        .id(concert.getId())
                        .title(concert.getTitle())
                        .venueName(concert.getVenue().getName())
                        .artist(concert.getArtist())
                        .status(concert.getStatus())
                        .startAt(concert.getStartAt())
                        .endAt(concert.getEndAt())
                        .thumbnailUrl(concert.getThumbnailUrl())
                        .build();
        }
}
