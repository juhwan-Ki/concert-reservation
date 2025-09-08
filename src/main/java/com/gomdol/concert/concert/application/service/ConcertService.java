package com.gomdol.concert.concert.application.service;

import com.gomdol.concert.concert.domain.Concert;
import com.gomdol.concert.concert.domain.repository.ConcertRepository;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.concert.presentation.dto.ShowResponse;
import com.gomdol.concert.show.domain.Show;
import com.gomdol.concert.show.domain.repository.ShowRepository;
import com.gomdol.concert.venue.domain.Venue;
import com.gomdol.concert.venue.domain.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {
    // TODO: 추후 port 사용하여 infra에 직접 연결하지 않도록 변경
    private final ConcertRepository concertRepository;
    private final VenueRepository venueRepository;
    private final ShowRepository showRepository;

    public ConcertDetailResponse getConcertById(Long id) {
        Concert concert = concertRepository.findById(id).orElseThrow(IllegalArgumentException::new);
        Venue venue = venueRepository.findByConcertId(concert.getId()).orElseThrow(IllegalArgumentException::new);
        List<Show> showList = showRepository.findByConcertId(concert.getId());
        List<ShowResponse> responseList = showList.stream()
                .map(ShowResponse::from)
                .toList();

        return ConcertDetailResponse.builder()
                .id(concert.getId())
                .title(concert.getTitle())
                .venueName(venue.getName())
                .artist(concert.getArtist())
                .ageRating(concert.getAgeRating())
                .runningTime(concert.getRunningTime())
                .description(concert.getDescription())
                .posterUrl(concert.getPosterUrl())
                .startAt(concert.getStartAt())
                .endAt(concert.getEndAt())
                .showList(responseList)
                .build();
    }
}
