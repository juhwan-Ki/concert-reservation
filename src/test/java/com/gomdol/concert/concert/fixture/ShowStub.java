package com.gomdol.concert.concert.fixture;

import com.gomdol.concert.show.infra.persistence.query.ShowProjection;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class ShowStub implements ShowProjection {
    private final Long id;
    private final String status;
    private final LocalDateTime showAt;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public LocalDateTime getShowAt() {
        return showAt;
    }

    @Override
    public String getStatus() {
        return status;
    }
}
