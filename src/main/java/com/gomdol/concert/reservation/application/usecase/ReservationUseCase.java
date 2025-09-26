package com.gomdol.concert.reservation.application.usecase;

import com.gomdol.concert.reservation.application.port.in.ReservationSeatCommand;
import com.gomdol.concert.reservation.application.port.out.ReservationCodeGenerator;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.application.port.out.ReservationSeatRepository;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.show.domain.repository.ShowQueryRepository;
import com.gomdol.concert.venue.application.port.out.VenueSeatRepository;
import com.gomdol.concert.venue.domain.model.VenueSeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository  reservationSeatRepository;
    private final VenueSeatRepository venueSeatRepository;
    private final ShowQueryRepository showQueryRepository;
    private final ReservationCodeGenerator reservationCodeGenerator;

    /**
     * 좌석 예약 (홀드)
     *
     * @param command 예약 요청 정보
     * @return 예약 결과
     */
    @Transactional
    public ReservationResponse reservationSeat(ReservationSeatCommand command) {
        log.info("reservation request: {}", command);
        // 같은 키로 들어오면 멱등성을 확인
        Optional<Reservation> existed = reservationRepository.findByRequestId(command.requestId());
        if(existed.isPresent())
            return ReservationResponse.fromDomain(existed.get());
        // 해당 공연이 존재하는지 확인
        if(!showQueryRepository.existsById(command.showId()))
            throw new IllegalArgumentException("공연이 존재하지 않습니다.");
        // 예약하려는 좌석이 존재하면 값을 가져옴
        List<VenueSeat> venueSeats = findSeatByIdsOrThrow(command.seatIds());
        // 좌석이 예약이 되어 있는지 확인
        checkReservableSeat(command.showId(), command.seatIds());
        // 예약이 안되어 있으면 예약(HOLD)
        long amount = venueSeats.stream().mapToLong(VenueSeat::getPrice).sum();
        String reservationCode = reservationCodeGenerator.newReservationCode();
        List<ReservationSeat> seats = venueSeats.stream().map(seat -> ReservationSeat.create(null, seat.getId(), command.showId())).toList();
        Reservation savedReservation = reservationRepository.save(Reservation.create(command.userId(), reservationCode, command.requestId(), seats, amount));

        log.info("좌석 예약 완료 - reservationId: {}, reservationCode: {}, holdExpiresAt: {}", savedReservation.getId(), savedReservation.getReservationCode(), savedReservation.getExpiresAt());
        return ReservationResponse.fromDomain(savedReservation);
    }

    private List<VenueSeat> findSeatByIdsOrThrow(List<Long> seatIds) {
        List<VenueSeat> venueSeats = venueSeatRepository.findByIds(seatIds);
        if(venueSeats.isEmpty())
            throw new IllegalStateException("좌석이 존재하지 않습니다.");

        return venueSeats;
    }

    private void checkReservableSeat(Long showId, List<Long> seatIds) {
        // 존재하면 이미 예약에 잡혀 있기 때문에 에러를 발생
        if(reservationSeatRepository.existsByShowIdAndIdIn(showId, seatIds))
            throw new IllegalStateException("일부 좌석이 이미 선점되었습니다.");
    }
}
