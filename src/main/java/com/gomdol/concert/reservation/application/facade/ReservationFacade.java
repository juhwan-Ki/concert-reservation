package com.gomdol.concert.reservation.application.facade;

import com.gomdol.concert.common.application.cache.port.out.CacheRepository;
import com.gomdol.concert.common.application.idempotency.service.IdempotencyService;
import com.gomdol.concert.common.application.lock.port.out.DistributedLock;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.common.infra.config.DistributedLockProperties;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.application.usecase.ReservationSeatUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.gomdol.concert.common.infra.config.DistributedLockProperties.*;
import static com.gomdol.concert.common.infra.util.CacheUtils.*;
import static com.gomdol.concert.reservation.application.port.in.ReservationSeatPort.*;

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
public class ReservationFacade {

    private final CacheRepository cacheRepository;
    private final DistributedLock distributedLock;
    private final DistributedLockProperties lockProperties;
    private final ReservationSeatUseCase reservationSeatUseCase;
    private final ReservationRepository reservationRepository;
    private final IdempotencyService idempotencyService;

    /**
     * 좌석 예약 with 멱등성 보장 및 분산 락
     * 1. Redis 캐시 체크
     * 2. DB 멱등성 체크
     * 3. 분산 락 획득
     * 4. UseCase 호출 (트랜잭션 시작)
     * 5. DB 제약조건 위반 시 멱등성 재확인
     */
    public ReservationResponse reservationSeat(ReservationSeatCommand command) {
        String cacheKey = reservationResult(command.requestId());

        // Redis 캐시 체크
        Optional<ReservationResponse> cached = cacheRepository.get(cacheKey, ReservationResponse.class);
        if (cached.isPresent()) {
            log.info("Redis 캐시 히트 - requestId={}", command.requestId());
            return cached.get();
        }

        // 좌석 기반 락만 사용 (좌석 중복 예약 방지)
        String seatLockKey = generateLockKey(command.showId(), command.seatIds());
        LockConfig lockConfig = lockProperties.reservation();

        return distributedLock.executeWithLock(seatLockKey, lockConfig.waitTime().toMillis(), lockConfig.leaseTime().toMillis(), TimeUnit.MILLISECONDS,
            () -> {
                ReservationResponse response = findByRequestId(command, cacheKey);
                if (response != null)
                    return response;

                return executeReservation(command, cacheKey);
            }
        );
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
            // 성공 시 캐시에 저장
            cacheRepository.set(cacheKey, response, RESERVATION_CACHE_TTL);
            log.info("캐시 저장 - requestId={}, reservationId={}", command.requestId(), response.reservationId());
            return response;
        } catch (DataIntegrityViolationException e) {
            log.warn("제약조건 위반 발생 - userId={} requestId={} error={}", command.userId(), command.requestId(), e.getMessage());
            // 멱등성 키로 다시 조회
            ReservationResponse response = findByRequestId(command, cacheKey);
            if(response != null)
                return response;
            // 멱등성 키가 없으면 좌석 중복 예약
            throw new IllegalStateException("이미 선택된 좌석입니다.");
        }
    }

    /**
     * 멱등키 조회
     * - 멱등키가 등록되어 있으면 동일한 값 반환
     */
    private ReservationResponse findByRequestId(ReservationSeatCommand command, String cacheKey) {
        return idempotencyService.findByIdempotencyKey(
                command.requestId(),
                command.userId(),
                ResourceType.RESERVATION,
                cacheKey,
                RESERVATION_CACHE_TTL,
                reservationRepository::findById,  // 엔티티 조회
                ReservationResponse::fromDomain   // 응답 변환
        );
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
