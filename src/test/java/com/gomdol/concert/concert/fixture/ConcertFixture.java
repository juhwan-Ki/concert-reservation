package com.gomdol.concert.concert.fixture;

import com.gomdol.concert.concert.domain.Concert;
import com.gomdol.concert.concert.domain.ConcertStatus;

import java.time.LocalDate;


public class ConcertFixture {

    public static Concert create() {
        return Concert.create(1L, "QWER 콘서트", "QWER", "QWER 단독 콘서트......","180분", "만 12이상",
                "https://asdasdasd1123easasdasd.jpg", "https://vkasjdkfljsdafbnalkdsjk.jpg",
                ConcertStatus.PUBLIC, LocalDate.of(2025,9,7),
                LocalDate.of(2025, 9, 10) );
    }
}
