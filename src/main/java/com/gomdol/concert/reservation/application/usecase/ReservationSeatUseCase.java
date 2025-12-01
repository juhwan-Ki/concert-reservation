package com.gomdol.concert.reservation.application.usecase;

import com.gomdol.concert.common.application.idempotency.port.in.CreateIdempotencyKey;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort.ReservationSeatCommand;
import com.gomdol.concert.reservation.application.port.out.ReservationCodeGenerator;
import com.gomdol.concert.reservation.application.port.out.ReservationPolicyProvider;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import com.gomdol.concert.show.application.port.out.ShowQueryRepository;
import com.gomdol.concert.venue.application.port.out.VenueSeatRepository;
import com.gomdol.concert.venue.domain.model.VenueSeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 좌석 예약 비즈니스 로직
 * - 트랜잭션 관리
 * - 멱등성 체크는 Facade에서 수행
 * - 순수 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationSeatUseCase {

    private final ReservationRepository reservationRepository;
    private final VenueSeatRepository venueSeatRepository;
    private final ShowQueryRepository showQueryRepository;
    private final ReservationCodeGenerator reservationCodeGenerator;
    private final ReservationPolicyProvider policyProvider;

    /**
     * 좌석 예약 (홀드)
     * - 단일 트랜잭션으로 실행
     * - 멱등성 체크는 Facade에서 수행
     *
     * @param command 예약 요청 정보
     * @return 예약 결과
     */
    @Transactional
    public ReservationResponse reservationSeat(ReservationSeatCommand command) {
        log.info("reservation request: {}", command);

        // 좌석 수 제한 검증
        if (command.seatIds().size() > policyProvider.maxSeatsPerReservation())
            throw new IllegalArgumentException(String.format("최대 %d개 좌석까지 예약 가능합니다.", policyProvider.maxSeatsPerReservation()));

        // 해당 공연이 존재하는지 확인
        if(!showQueryRepository.existsById(command.showId()))
            throw new IllegalArgumentException("공연이 존재하지 않습니다.");

        // 예약하려는 좌석이 존재하면 값을 가져옴
        List<Long> sortedSeatIds = command.seatIds().stream().sorted().toList();
        List<VenueSeat> venueSeats = findSeatByIdsOrThrow(sortedSeatIds);

        // 예약 생성 (유니크 제약조건이 동시성 제어)
        long amount = venueSeats.stream().mapToLong(VenueSeat::getPrice).sum();
        String reservationCode = reservationCodeGenerator.newReservationCode();
        List<ReservationSeat> seats = venueSeats.stream() // 데드락 발생을 줄이기 위해 정렬함
                .sorted(Comparator.comparing(VenueSeat::getVenueId))  // 다시 한번 확인
                .map(seat -> ReservationSeat.create(null, seat.getId(), command.showId()))
                .toList();

        // 예외를 던져서 Facade에서 처리하도록 함 (트랜잭션 rollback-only 문제 방지)
        Reservation savedReservation = reservationRepository.save(
                Reservation.create(command.userId(), reservationCode, command.requestId(), seats, amount, policyProvider.holdMinutes()));

        log.info("좌석 예약 완료 - reservationId: {}, reservationCode: {}, holdExpiresAt: {}",
                savedReservation.getId(), savedReservation.getReservationCode(), savedReservation.getExpiresAt());
        return ReservationResponse.fromDomain(savedReservation);
    }

    private List<VenueSeat> findSeatByIdsOrThrow(List<Long> seatIds) {
        List<VenueSeat> venueSeats = venueSeatRepository.findByIds(seatIds);
        if(venueSeats.isEmpty())
            throw new IllegalStateException("좌석이 존재하지 않습니다.");

        return venueSeats;
    }
}
