package com.gomdol.concert.concert.application.eventhandler;

import com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository;
import com.gomdol.concert.concert.application.service.FastSellingRankingService;
import com.gomdol.concert.concert.domain.event.RankingUpdateRequestedEvent;
import com.gomdol.concert.payment.domain.event.PaymentCompletedEvent;
import com.gomdol.concert.show.application.port.out.ShowRepository;
import com.gomdol.concert.show.domain.model.Show;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 랭킹 이벤트 핸들러
 * - 랭킹 업데이트 요청 이벤트를 받아 랭킹 업데이트
 * - 좌석 확정 시점에 랭킹 반영
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastSellingEventHandler {

    private final FastSellingRankingService rankingService;

    /**
     * 랭킹 업데이트 요청 이벤트 처리
     * - 트랜잭션 커밋 후 실행
     * - showId로 concert 정보 조회 후 랭킹 업데이트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRankingUpdateRequested(RankingUpdateRequestedEvent event) {
        try {
            log.info("랭킹 업데이트 요청 수신 - reservationId={}, concertId={}, seatCount={}",
                    event.getReservationId(), event.getConcertId(), event.getSeatCount());
            rankingService.rankingUpdate(event);
        } catch (Exception e) {
            log.error("랭킹 업데이트 실패 - reservationId={}, error={}", event.getReservationId(), e.getMessage(), e);
            // 실패해도 예약은 유지 (비동기 처리이므로 예외를 전파하지 않음)
        }
    }
}
