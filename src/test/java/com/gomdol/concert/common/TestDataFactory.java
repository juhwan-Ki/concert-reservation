package com.gomdol.concert.common;

import com.gomdol.concert.concert.domain.model.AgeRating;
import com.gomdol.concert.concert.domain.model.ConcertStatus;
import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import com.gomdol.concert.concert.infra.persistence.command.ConcertJpaRepository;
import com.gomdol.concert.show.domain.model.ShowStatus;
import com.gomdol.concert.show.infra.persistence.entity.ShowEntity;
import com.gomdol.concert.show.infra.persistence.command.ShowJpaRepository;
import com.gomdol.concert.venue.infra.persistence.VenueJpaRepository;
import com.gomdol.concert.venue.infra.persistence.VenueSeatJpaRepository;
import com.gomdol.concert.venue.infra.persistence.entity.VenueEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueSeatEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TestDataFactory {

    private final VenueJpaRepository venueRepository;
    private final VenueSeatJpaRepository venueSeatRepository;
    private final ConcertJpaRepository concertRepository;
    private final ShowJpaRepository showRepository;

    public VenueEntity createVenue(String name, String address, int capacity) {
        VenueEntity venue = VenueEntity.create(name, address, capacity);
        return venueRepository.save(venue);
    }

    public VenueSeatEntity createVenueSeat(VenueEntity venue, String rowLabel, int seatNumber, long price) {
        String seatLabel = rowLabel.toUpperCase() + "-" + seatNumber;
        VenueSeatEntity seat = VenueSeatEntity.create(seatLabel, rowLabel, seatNumber, price, venue);
        return venueSeatRepository.save(seat);
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
        return concertRepository.save(concert);
    }

    public ShowEntity createShow(ConcertEntity concert, LocalDateTime showAt, int capacity) {
        ShowEntity show = ShowEntity.create(concert, showAt, ShowStatus.ON_SALE, capacity);
        return showRepository.save(show);
    }
}