package com.gomdol.concert.concert.fixture;

import com.gomdol.concert.concert.infra.persistence.query.ConcertDetailProjection;
import com.gomdol.concert.show.infra.persistence.query.ShowProjection;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public class ConcertDetailStub implements ConcertDetailProjection {
    private final Long id;
    private final String title;
    private final String description;
    private final String venueName;
    private final String ageRating;
    private final Integer runningTime;
    private final String artist;
    private final String posterUrl;
    private final LocalDate startAt;
    private final LocalDate endAt;
    private final LocalDateTime deletedAt;
    private final List<ShowProjection> shows;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVenueName() {
        return venueName;
    }

    @Override
    public String getAgeRating() {
        return ageRating;
    }

    @Override
    public Integer getRunningTime() {
        return runningTime;
    }

    @Override
    public String getArtist() {
        return artist;
    }

    @Override
    public String getPosterUrl() {
        return posterUrl;
    }

    @Override
    public LocalDate getStartAt() {
        return startAt;
    }

    @Override
    public LocalDate getEndAt() {
        return endAt;
    }

    @Override
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
