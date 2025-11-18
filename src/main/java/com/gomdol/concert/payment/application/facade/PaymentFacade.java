package com.gomdol.concert.payment.application.facade;

import com.gomdol.concert.payment.application.port.in.PaymentPort;
import com.gomdol.concert.payment.application.usecase.PaymentQueryUseCase;
import com.gomdol.concert.payment.application.usecase.PaymentUseCase;
import com.gomdol.concert.payment.domain.model.Payment;
import com.gomdol.concert.payment.presentation.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 결제 작업 Facade
 * - 재시도 로직 처리 (동시성 이슈로 인한 일시적 실패 대응)
 * - 실제 비즈니스 로직은 PaymentUseCase 위임 (REQUIRES_NEW 트랜잭션)
 * - 추후 분산락 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade implements PaymentPort {

    private final PaymentUseCase paymentUseCase;
    private final PaymentQueryUseCase paymentQueryUseCase;

    @Override
    public PaymentResponse processPayment(PaymentCommand command) {
        String requestId = command.requestId();
        String userId = command.userId();
        int maxRetries = 3;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                //  REQUIRES_NEW 트랜잭션으로 실행됨
                return paymentUseCase.processPayment(command);
            } catch (DataIntegrityViolationException e) {
                // 유니크 제약조건 위반 → 멱등성 체크
                log.warn("제약조건 위반 발생 - userId={} requestId={} attempt={} error={}", userId, requestId, attempt + 1, e.getMessage());

                // 새로운 트랜잭션에서 조회하여 멱등성 체크
                Optional<Payment> existed = paymentQueryUseCase.findByRequestId(requestId);
                if (existed.isPresent()) {
                    log.info("멱등성 보장: 기존 결제 반환 - requestId={}, paymentCode={}", requestId, existed.get().getPaymentCode());
                    return PaymentResponse.fromDomain(existed.get());
                }

                throw new IllegalStateException("이미 결제가 완료되었습니다.");
            } catch (IllegalStateException e) {
                // 기타 동시성 이슈로 인한 일시적 실패 → 재시도
                if (attempt == maxRetries - 1) {
                    log.error("결제 저장 실패 - 최대 재시도 횟수 초과 userId={} requestId={} attempt={}", userId, requestId, attempt + 1, e);
                    throw new IllegalStateException("처리 중인 요청이 너무 많습니다. 잠시 후 다시 시도하세요.", e);
                }

                log.warn("결제 저장 재시도 중 userId={} requestId={} attempt={}/{} exception={}",
                        userId, requestId, attempt + 1, maxRetries, e.getClass().getSimpleName());

                try {
                    Thread.sleep(100 * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("재시도 중 인터럽트 발생", ie);
                }
            }
        }
        throw new IllegalStateException("결제 저장 실패");
    }
}
