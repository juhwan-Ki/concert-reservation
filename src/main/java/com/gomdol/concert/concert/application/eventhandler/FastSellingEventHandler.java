package com.gomdol.concert.concert.application.eventhandler;

import com.gomdol.concert.concert.application.service.FastSellingRankingService;
import com.gomdol.concert.payment.domain.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 랭킹 이벤트 핸들러
 * - 결제 완료 이벤트를 받아 랭킹 업데이트
 * - 결제 완료 시점에만 랭킹 반영
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastSellingEventHandler {

    private final FastSellingRankingService rankingService;

    /**
     * 결제 완료 이벤트 처리
     * - 트랜잭션 커밋 후 실행 (결제 트랜잭션 완료 후)
     * - 이벤트 정보를 그대로 전달
     *
     * @param event 결제 완료 이벤트 (공연 정보 포함)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("결제 완료 이벤트 수신 (랭킹 업데이트) - paymentId={}, reservationId={}, showId={}, title={}, seatCount={}",
                    event.paymentId(), event.reservationId(), event.showId(), event.concertTitle(), event.seatCount());

            rankingService.handlePaymentCompleted(event);

            log.info("랭킹 업데이트 완료 - showId={}", event.showId());
        } catch (Exception e) {
            log.error("랭킹 업데이트 실패 - showId={}, error={}", event.showId(), e.getMessage(), e);
            // 실패해도 결제는 유지 (비동기 처리이므로 예외를 전파하지 않음)
        }
    }
}
