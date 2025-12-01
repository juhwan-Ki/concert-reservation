package com.gomdol.concert.reservation.application.facade;

import com.gomdol.concert.common.application.cache.port.out.CacheRepository;
import com.gomdol.concert.common.application.idempotency.port.in.CreateIdempotencyKey;
import com.gomdol.concert.common.application.idempotency.port.in.GetIdempotencyKey;
import com.gomdol.concert.common.application.lock.port.out.DistributedLock;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.application.usecase.ReservationSeatUseCase;
import com.gomdol.concert.reservation.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 예약 작업 Facade
 * - Redis 캐시로 빠른 멱등성 체크
 * - DB 멱등키로 영속적 멱등성 보장
 * - Redis 분산 락으로 동시성 제어
 * - 단일 트랜잭션으로 비즈니스 로직 실행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationFacade implements ReservationSeatPort {

    private final CacheRepository cacheRepository;
    private final GetIdempotencyKey getIdempotencyKey;
    private final CreateIdempotencyKey createIdempotencyKey;
    private final DistributedLock distributedLock;
    private final ReservationSeatUseCase reservationSeatUseCase;
    private final ReservationRepository reservationRepository;

    private static final String CACHE_KEY_PREFIX = "reservation:result:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /**
     * 좌석 예약 with 멱등성 보장 및 분산 락
     * 1. Redis 캐시 체크
     * 2. DB 멱등성 체크
     * 3. 분산 락 획득
     * 4. UseCase 호출 (트랜잭션 시작)
     * 5. DB 제약조건 위반 시 멱등성 재확인
     */
    public ReservationResponse reservationSeat(ReservationSeatCommand command) {
        String cacheKey = CACHE_KEY_PREFIX + command.requestId();

        // Redis 캐시 체크
        Optional<ReservationResponse> cached = cacheRepository.get(cacheKey, ReservationResponse.class);
        if (cached.isPresent()) {
            log.info("Redis 캐시 히트 - requestId={}", command.requestId());
            return cached.get();
        }

        // requestId 기반 락으로 동일 요청 직렬화 (멱등성 보장)
        String requestLockKey = "reservation:request:" + command.requestId();
        return distributedLock.executeWithLock(requestLockKey, 3, 10, TimeUnit.SECONDS, () -> {
            // requestId 락 내에서 멱등키 재확인
            Optional<Long> existingReservationId = getIdempotencyKey.getIdempotencyKey(
                    command.requestId(),
                    command.userId(),
                    ResourceType.RESERVATION
            );

            if (existingReservationId.isPresent()) {
                log.info("requestId 락 내 멱등키 발견: 기존 예약 반환 - requestId={}, reservationId={}",
                        command.requestId(), existingReservationId.get());
                Reservation reservation = reservationRepository.findById(existingReservationId.get())
                        .orElseThrow(() -> new IllegalStateException("멱등성 키는 존재하나 예약을 찾을 수 없습니다."));
                ReservationResponse response = ReservationResponse.fromDomain(reservation);
                cacheRepository.set(cacheKey, response, CACHE_TTL);
                return response;
            }

            // 좌석 기반 락으로 동시성 제어 (좌석 중복 예약 방지)
            String seatLockKey = generateLockKey(command.showId(), command.seatIds());
            return distributedLock.executeWithLock(seatLockKey, 3, 10, TimeUnit.SECONDS,
                    () -> executeReservation(command, cacheKey));
        });
    }

    /**
     * 예약 처리
     * - 성공 시 Redis 캐시에 저장
     * - 이미 처리된 요청이면 기존 예약 반환 및 캐시 저장
     */
    private ReservationResponse executeReservation(ReservationSeatCommand command, String cacheKey) {
        try {
            log.info("예약 처리 시작 - userId={}, requestId={}", command.userId(), command.requestId());
            ReservationResponse response = reservationSeatUseCase.reservationSeat(command);
            // 멱등성 키 저장 - 성공적으로 처리된 요청 기록
            createIdempotencyKey.createIdempotencyKey(
                    command.requestId(),
                    command.userId(),
                    ResourceType.RESERVATION,
                    response.reservationId()
            );
            // 성공 시 캐시에 저장
            cacheRepository.set(cacheKey, response, CACHE_TTL);
            log.info("예약 성공 및 캐시 저장 - requestId={}, reservationId={}", command.requestId(), response.reservationId());

            return response;
        } catch (DataIntegrityViolationException e) {
            // DB 제약조건 위반 (멱등성 키 or 좌석 중복) → 멱등성 재확인
            log.warn("제약조건 위반 발생 - userId={} requestId={} error={}", command.userId(), command.requestId(), e.getMessage());

            // 멱등성 키로 다시 조회
            Optional<Long> existingReservationId = getIdempotencyKey.getIdempotencyKey(
                    command.requestId(),
                    command.userId(),
                    ResourceType.RESERVATION
            );

            if (existingReservationId.isPresent()) {
                log.info("멱등성 보장: 기존 예약 반환 - requestId={}, reservationId={}", command.requestId(), existingReservationId.get());
                Reservation reservation = reservationRepository.findById(existingReservationId.get())
                        .orElseThrow(() -> new IllegalStateException("멱등성 키는 존재하나 예약을 찾을 수 없습니다."));
                ReservationResponse response = ReservationResponse.fromDomain(reservation);
                // 캐시에 저장 (다음 요청을 위해)
                cacheRepository.set(cacheKey, response, CACHE_TTL);
                return response;
            }
            // 멱등성 키가 없으면 좌석 중복 예약
            throw new IllegalStateException("이미 선택된 좌석입니다.");
        }
    }

    /**
     * 락 키 생성: reservation:show:{showId}:seats:{sorted-seatIds}
     */
    private String generateLockKey(Long showId, List<Long> seatIds) {
        String sortedSeats = seatIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return String.format("reservation:show:%d:seats:%s", showId, sortedSeats);
    }
}
