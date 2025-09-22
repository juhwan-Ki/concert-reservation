package com.gomdol.concert.concert.fixture;

import com.gomdol.concert.venue.domain.model.Venue;

public class VenueFixture {
    public static Venue create() {
        return Venue.create(1L, "인천 인스파이어 아레나", "인천광역시 중구 공항문화로 127", 25000);
    }
}
