package com.gomdol.concert.payment.application.facade;

import com.gomdol.concert.common.application.cache.port.out.CacheRepository;
import com.gomdol.concert.common.application.idempotency.service.IdempotencyService;
import com.gomdol.concert.common.application.lock.port.out.DistributedLock;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.common.infra.config.DistributedLockProperties;
import com.gomdol.concert.payment.application.port.in.SavePaymentPort.PaymentCommand;
import com.gomdol.concert.payment.application.usecase.PaymentQueryUseCase;
import com.gomdol.concert.payment.application.usecase.SavePaymentUseCase;
import com.gomdol.concert.payment.presentation.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.gomdol.concert.common.infra.config.DistributedLockProperties.*;
import static com.gomdol.concert.common.infra.util.CacheUtils.*;

/**
 * 결제 작업 Facade
 * - Redis 캐시로 빠른 멱등성 체크
 * - DB 멱등키로 영속적 멱등성 보장
 * - Redis 분산 락으로 동시성 제어
 * - 단일 트랜잭션으로 비즈니스 로직 실행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final CacheRepository cacheRepository;
    private final DistributedLock distributedLock;
    private final DistributedLockProperties lockProperties;
    private final SavePaymentUseCase paymentUseCase;
    private final PaymentQueryUseCase paymentQueryUseCase;
    private final IdempotencyService idempotencyService;

    /**
     * 결제 처리 with 멱등성 보장 및 분산 락
     * 1. Redis 캐시 체크
     * 2. DB 멱등성 체크 (트랜잭션 전)
     * 3. 분산 락 획득
     * 4. UseCase 호출 (트랜잭션 시작)
     * 5. DB 제약조건 위반 시 멱등성 재확인
     */
    public PaymentResponse processPayment(PaymentCommand command) {
        String cacheKey = paymentResult(command.requestId());

        // Redis 캐시 체크 (가장 빠른 조회)
        Optional<PaymentResponse> cached = cacheRepository.get(cacheKey, PaymentResponse.class);
        if (cached.isPresent()) {
            log.info("Redis 캐시 히트 - requestId={}", command.requestId());
            return cached.get();
        }

        // DB 멱등성 체크 (트랜잭션 전) - 이미 처리된 요청이면 즉시 반환
        PaymentResponse response = findByRequestId(command, cacheKey);
        if (response != null)
            return response;

        // 분산 락 획득 및 UseCase 호출
        String lockKey = generateLockKey(command.reservationId());
        LockConfig lockConfig = lockProperties.payment();

        return distributedLock.executeWithLock(
            lockKey,
            lockConfig.waitTime().toMillis(),
            lockConfig.leaseTime().toMillis(),
            TimeUnit.MILLISECONDS,
            () -> executePayment(command, cacheKey)
        );
    }

    /**
     * 결제 처리
     * - 성공 시 Redis 캐시에 저장
     * - DB 제약조건 위반 시 멱등성 키로 재확인
     * - 이미 처리된 요청이면 기존 결제 반환 및 캐시 저장
     */
    private PaymentResponse executePayment(PaymentCommand command, String cacheKey) {
        try {
            log.info("결제 처리 시작 - userId={}, requestId={}, reservationId={}", command.userId(), command.requestId(), command.reservationId());
            PaymentResponse response = paymentUseCase.processPayment(command);
            // 성공 시 캐시에 저장
            cacheRepository.set(cacheKey, response, PAYMENT_CACHE_TTL);
            log.info("캐시 저장 - requestId={}, paymentId={}", command.requestId(), response.paymentId());
            return response;
        } catch (DataIntegrityViolationException e) {
            log.warn("제약조건 위반 발생 - userId={} requestId={} error={}", command.userId(), command.requestId(), e.getMessage());
            PaymentResponse response = findByRequestId(command, cacheKey);
            if (response != null)
                return response;
            // 멱등성 키가 없으면 다른 제약조건 위반
            throw new IllegalStateException("결제 처리 중 제약조건 위반", e);
        }
    }

    /**
     * 멱등키 조회
     * - 멱등키가 등록되어 있으면 동일한 값 반환
     */
    private PaymentResponse findByRequestId(PaymentCommand command, String cacheKey) {
        return idempotencyService.findByIdempotencyKey(
                command.requestId(),
                command.userId(),
                ResourceType.PAYMENT,
                cacheKey,
                PAYMENT_CACHE_TTL,
                paymentQueryUseCase::findById,  // 엔티티 조회
                PaymentResponse::fromDomain   // 응답 변환
        );
    }

    /**
     * 락 키 생성: payment:reservation:{reservationId}
     * - 같은 예약에 대한 결제는 순차적으로 처리
     */
    private String generateLockKey(Long reservationId) {
        return String.format("payment:reservation:%d", reservationId);
    }
}
