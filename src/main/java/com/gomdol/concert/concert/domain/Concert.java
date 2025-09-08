package com.gomdol.concert.concert.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class Concert {
    private final Long id;
    private final String title;
    private final String artist;
    private final String description;
    private final String runningTime;
    private final String ageRating;
    private final String thumbnailUrl;
    private final String posterUrl;
    private final ConcertStatus status;
    private final LocalDate startAt;
    private final LocalDate endAt;

    private Concert (Long id, String title, String artist, String description, String runningTime, String ageRating, String thumbnailUrl, String posterUrl, ConcertStatus status, LocalDate startAt, LocalDate endAt) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.description = description;
        this.runningTime = runningTime;
        this.ageRating = ageRating;
        this.thumbnailUrl = thumbnailUrl;
        this.posterUrl = posterUrl;
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static Concert create(Long id, String title, String artist, String description, String runningTime, String ageRating, String thumbnailUrl, String posterUrl, ConcertStatus status, LocalDate startAt, LocalDate endAt ) {
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
