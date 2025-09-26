package com.gomdol.concert.show.infra.query.projection;

import java.time.LocalDateTime;

public interface ShowProjection {
    Long getId();
    LocalDateTime getShowAt();
    String getStatus();
}
