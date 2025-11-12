package com.gomdol.concert.reservation.application.usecase;

import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort;
import com.gomdol.concert.reservation.application.port.out.ReservationCodeGenerator;
import com.gomdol.concert.reservation.application.port.out.ReservationPolicyProvider;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.show.application.port.out.ShowQueryRepository;
import com.gomdol.concert.venue.application.port.out.VenueSeatRepository;
import com.gomdol.concert.venue.domain.model.VenueSeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationSeatUseCase implements ReservationSeatPort {

    private final ReservationRepository reservationRepository;
    private final VenueSeatRepository venueSeatRepository;
    private final ShowQueryRepository showQueryRepository;
    private final ReservationCodeGenerator reservationCodeGenerator;
    private final ReservationPolicyProvider policyProvider;

    /**
     * 좌석 예약 (홀드)
     *
     * @param command 예약 요청 정보
     * @return 예약 결과
     */
    public ReservationResponse reservationSeat(ReservationSeatCommand command) {
        log.info("reservation request: {}", command);

        // 같은 키로 들어오면 멱등성을 확인
        Optional<Reservation> existed = reservationRepository.findByRequestId(command.requestId());
        if (existed.isPresent()) {
            log.info("멱등성 체크: 기존 예약 반환 - requestId={}, reservationCode={}", command.requestId(), existed.get().getReservationCode());
            return ReservationResponse.fromDomain(existed.get());
        }

        // 좌석 수 제한 검증 추가
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
                .sorted(Comparator.comparing(VenueSeat::getId))  // 다시 한번 확인
                .map(seat -> ReservationSeat.create(null, seat.getId(), command.showId()))
                .toList();

        try {
            Reservation savedReservation = reservationRepository.save(
                    Reservation.create(command.userId(), reservationCode, command.requestId(), seats, amount, policyProvider.holdMinutes()));

            log.info("좌석 예약 완료 - reservationId: {}, reservationCode: {}, holdExpiresAt: {}",
                    savedReservation.getId(), savedReservation.getReservationCode(), savedReservation.getExpiresAt());
            return ReservationResponse.fromDomain(savedReservation);
        } catch (DataIntegrityViolationException e) {
            log.warn("좌석 예약 실패 (제약조건 위반): showId={}, seatIds={}, error={}", command.showId(), sortedSeatIds, e.getMessage());
            return handleIdempotencyConflict(command.requestId(), e);
        }
    }

    private List<VenueSeat> findSeatByIdsOrThrow(List<Long> seatIds) {
        List<VenueSeat> venueSeats = venueSeatRepository.findByIds(seatIds);
        if(venueSeats.isEmpty())
            throw new IllegalStateException("좌석이 존재하지 않습니다.");

        return venueSeats;
    }

    private ReservationResponse handleIdempotencyConflict(String requestId, DataIntegrityViolationException e) {
        // 유니크 제약조건 위반
        // 1. requestId 중복인 경우: 멱등성 보장을 위해 기존 예약 반환
        // 2. 좌석 중복인 경우: 예외 발생
        String errorMessage = e.getMessage().toLowerCase();

        // requestId 중복 확인
        if (errorMessage.contains("request_id") || errorMessage.contains("uk_request")) {
            log.info("멱등성 보장: requestId 중복 - requestId={}", requestId);
            // 재조회 (이미 커밋된 상태이므로 조회 가능)
            return reservationRepository.findByRequestId(requestId)
                    .map(ReservationResponse::fromDomain)
                    .orElseThrow(() -> new IllegalStateException("예약이 생성되었으나 조회할 수 없습니다."));
        }

        // 좌석 중복
        if (errorMessage.contains("seat") || errorMessage.contains("uk_show_seat")) {
            log.warn("좌석 중복 - requestId={}", requestId);
            throw new IllegalStateException("이미 선택된 좌석입니다.");
        }

        throw new IllegalStateException("예약 처리 중 오류 발생", e);
    }
}
