package com.gomdol.concert.show.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Show {
    private final Long id;
    private final ShowStatus status;
    private final LocalDateTime showAt;

    private Show(Long id, ShowStatus status, LocalDateTime showAt) {
        this.id = id;
        this.status = status;
        this.showAt = showAt;
    }

    public static Show create(Long id, ShowStatus status, LocalDateTime showAt) {
        return new Show(id, status, showAt);
    }
}
