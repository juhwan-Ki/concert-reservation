package com.gomdol.concert.concert.fixture;

import com.gomdol.concert.show.domain.model.Show;
import com.gomdol.concert.show.domain.model.ShowStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ShowFixture {
    public static List<Show> createList() {
        return List.of(
                Show.create(1L, 1L, ShowStatus.ON_SALE, LocalDateTime.of(2025,9, 7, 17,0), "Concert A", "Venue A", 100, 0),
                Show.create(2L, 2L, ShowStatus.ON_SALE, LocalDateTime.of(2025,9, 8, 17,0), "Concert B", "Venue B", 100, 0),
                Show.create(3L, 3L, ShowStatus.ON_SALE, LocalDateTime.of(2025,9, 9, 17,0), "Concert C", "Venue C", 100, 0),
                Show.create(4L, 4L, ShowStatus.ON_SALE, LocalDateTime.of(2025,9, 10, 17,0), "Concert D", "Venue D", 100, 0)
        );
    }
}
