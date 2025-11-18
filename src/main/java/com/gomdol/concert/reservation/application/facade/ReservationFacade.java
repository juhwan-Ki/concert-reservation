package com.gomdol.concert.reservation.application.facade;

import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort;
import com.gomdol.concert.reservation.application.port.out.ReservationPolicyProvider;
import com.gomdol.concert.reservation.application.usecase.ReservationQueryUseCase;
import com.gomdol.concert.reservation.application.usecase.ReservationSeatUseCase;
import com.gomdol.concert.reservation.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 예약 작업 Facade
 * - 재시도 로직 처리 (동시성 이슈로 인한 일시적 실패 대응)
 * - 실제 비즈니스 로직은 ReservationSeatUseCase에 위임 (REQUIRES_NEW 트랜잭션)
 * - 추후 분산락 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationFacade implements ReservationSeatPort {

    private final ReservationSeatUseCase reservationSeatUseCase;
    private final ReservationQueryUseCase reservationQueryService;
    private final ReservationPolicyProvider reservationPolicyProvider;

    // TODO: 추후 레디스 적용 예정
    @Override
    public ReservationResponse reservationSeat(ReservationSeatCommand command) {
        String requestId = command.requestId();
        String userId = command.userId();
        int maxRetries = reservationPolicyProvider.maxRetryCount();

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return reservationSeatUseCase.reservationSeat(command);
            } catch (DataIntegrityViolationException e) {
                // 유니크 제약조건 위반 → 멱등성 체크
                log.warn("제약조건 위반 발생 - userId={} requestId={} attempt={} error={}",
                        userId, requestId, attempt + 1, e.getMessage());

                // 새로운 트랜잭션에서 조회하여 멱등성 체크
                Optional<Reservation> existed = reservationQueryService.findByRequestId(requestId);
                if (existed.isPresent()) {
                    log.info("멱등성 보장: 기존 예약 반환 - requestId={}, reservationCode={}",
                            requestId, existed.get().getReservationCode());
                    return ReservationResponse.fromDomain(existed.get());
                }
                // requestId로 조회되지 않으면 좌석 중복이기 때문에 에러를 던짐
                throw new IllegalStateException("이미 선택된 좌석입니다.");
            } catch (IllegalStateException e) {
                // 기타 동시성 이슈로 인한 일시적 실패 → 재시도
                if (attempt == maxRetries - 1) {
                    log.error("예약 저장 실패 - 최대 재시도 횟수 초과 userId={} requestId={} attempt={}",
                            userId, requestId, attempt + 1, e);
                    throw new IllegalStateException("처리 중인 요청이 너무 많습니다. 잠시 후 다시 시도하세요.", e);
                }

                log.warn("예약 저장 재시도 중 userId={} requestId={} attempt={}/{} exception={}",
                        userId, requestId, attempt + 1, maxRetries, e.getClass().getSimpleName());

                try {
                    Thread.sleep(reservationPolicyProvider.retryDelayMillis() * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("재시도 중 인터럽트 발생", ie);
                }
            }
        }
        throw new IllegalStateException("예약 저장 실패");
    }
}
