package com.gomdol.concert.concert.fixture;

import com.gomdol.concert.show.domain.Show;
import com.gomdol.concert.show.domain.ShowStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ShowFixture {
    public static List<Show> createList() {
        return List.of(
                Show.create(1L, ShowStatus.ON_SALE ,LocalDateTime.of(2025,9, 7, 17,0)),
                Show.create(2L, ShowStatus.ON_SALE ,LocalDateTime.of(2025,9, 8, 17,0)),
                Show.create(3L, ShowStatus.ON_SALE ,LocalDateTime.of(2025,9, 9, 17,0)),
                Show.create(4L, ShowStatus.ON_SALE ,LocalDateTime.of(2025,9, 10, 17,0))
        );
    }
}
