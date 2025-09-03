package com.gomdol.concert.concert.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class Concert {
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
}
