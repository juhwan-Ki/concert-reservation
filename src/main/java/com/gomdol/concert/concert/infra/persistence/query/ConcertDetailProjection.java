package com.gomdol.concert.concert.infra.persistence.query;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ConcertDetailProjection {
    Long getId();
    String getTitle();
    String getVenueName();
    String getArtist();
    String getAgeRating();
    Integer getRunningTime();
    String getDescription();
    String getPosterUrl();
    LocalDate getStartAt();
    LocalDate getEndAt();
    LocalDateTime getDeletedAt();
}
