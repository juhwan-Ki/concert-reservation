package com.gomdol.concert.concert.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class Concert {
    private final Long id;
    private final String title;
    private final String artist;
    private final String description;
    private final int runningTime;
    private final AgeRating ageRating;
    private final String thumbnailUrl;
    private final String posterUrl;
    private final ConcertStatus status;
    private final LocalDate startAt;
    private final LocalDate endAt;
    private final LocalDateTime deleteAt;

    public static Concert create(Long id, String title, String artist, String description, int runningTime, AgeRating ageRating, String thumbnailUrl, String posterUrl, ConcertStatus status, LocalDate startAt, LocalDate endAt ) {
        return Concert.builder()
                .id(id)
                .title(title)
                .artist(artist)
                .description(description)
                .runningTime(runningTime)
                .ageRating(ageRating)
                .thumbnailUrl(thumbnailUrl)
                .posterUrl(posterUrl)
                .status(status)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }
}
