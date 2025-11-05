package com.gomdol.concert.common;

import com.gomdol.concert.concert.domain.model.AgeRating;
import com.gomdol.concert.concert.domain.model.ConcertStatus;
import com.gomdol.concert.concert.infra.command.persistence.ConcertEntity;
import com.gomdol.concert.show.domain.ShowStatus;
import com.gomdol.concert.show.infra.command.persistence.ShowEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueSeatEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class TestDataFactory {

    private final EntityManager entityManager;

    public TestDataFactory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public VenueEntity createVenue(String name, String address, int capacity) {
        VenueEntity venue = VenueEntity.create(name, address, capacity);
        entityManager.persist(venue);
        entityManager.flush();
        return venue;
    }

    public VenueSeatEntity createVenueSeat(VenueEntity venue, String rowLabel, int seatNumber, long price) {
        String seatLabel = rowLabel.toUpperCase() + "-" + seatNumber;
        VenueSeatEntity seat = VenueSeatEntity.create(seatLabel, rowLabel, seatNumber, price, venue);
        entityManager.persist(seat);
        entityManager.flush();
        return seat;
    }

    public ConcertEntity createConcert(String title, VenueEntity venue) {
        ConcertEntity concert = ConcertEntity.create(
                title,
                "Test Artist",
                "Test Description",
                120,
                AgeRating.ALL,
                null,
                null,
                ConcertStatus.PUBLIC,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                venue
        );
        entityManager.persist(concert);
        entityManager.flush();
        return concert;
    }

    public ShowEntity createShow(ConcertEntity concert, LocalDateTime showAt, int capacity) {
        ShowEntity show = ShowEntity.create(concert, showAt, ShowStatus.ON_SALE, capacity);
        entityManager.persist(show);
        entityManager.flush();
        return show;
    }
}