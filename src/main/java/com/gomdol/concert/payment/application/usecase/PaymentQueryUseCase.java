package com.gomdol.concert.payment.application.usecase;

import com.gomdol.concert.payment.application.port.out.PaymentRepository;
import com.gomdol.concert.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 결제 조회 전용 서비스
 * - 멱등성 체크를 위한 별도 트랜잭션 조회
 * - REQUIRES_NEW로 새 트랜잭션에서 실행하여 커밋된 데이터 조회 보장
 * - 추후 결제 조회 관련 기능 추가 예정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryUseCase {

    private final PaymentRepository paymentRepository;

    /**
     * requestId로 예약 조회 (멱등성 체크용)
     * 새로운 읽기 전용 트랜잭션에서 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Payment> findByRequestId(String requestId) {
        return paymentRepository.findByRequestId(requestId);
    }
}
