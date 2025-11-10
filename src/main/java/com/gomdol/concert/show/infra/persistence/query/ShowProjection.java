package com.gomdol.concert.show.infra.persistence.query;

import java.time.LocalDateTime;

public interface ShowProjection {
    Long getId();
    LocalDateTime getShowAt();
    String getStatus();
}
