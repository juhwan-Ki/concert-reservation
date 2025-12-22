package com.gomdol.concert.concert.application.service;

import com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository;
import com.gomdol.concert.concert.domain.event.RankingUpdateRequestedEvent;
import com.gomdol.concert.concert.domain.model.FastSellingConcert;
import com.gomdol.concert.payment.domain.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 랭킹 서비스
 * - 공연 랭킹 업데이트 (이벤트 기반)
 * - 결제 완료 시점에만 랭킹 반영
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FastSellingRankingService {

    private final FastSellingRankingRepository rankingRepository;

    /**
     * 랭킹 업데이트 요청 이벤트 처리
     * - 트랜잭션 커밋 후 실행
     * - 이벤트에서 받은 정보로 Redis 업데이트
     * - 시간당 판매량 증가
     * - 랭킹 점수 계산 및 업데이트
     */
    public void rankingUpdate(RankingUpdateRequestedEvent event) {
        try {
            log.info("랭킹 업데이트 요청 수신 - reservationId={}, concertId={}, title={}, seatCount={}",
                    event.getReservationId(), event.getConcertId(), event.getConcertTitle(), event.getSeatCount());

            // 1. 시간당 판매량 증가 (콘서트 단위)
            rankingRepository.incrementHourlySales(event.getConcertId());

            // 2. Redis에 콘서트 통계 저장 (이벤트 정보 사용)
            rankingRepository.saveConcertStats(event.getConcertId(), event.getConcertTitle(), event.getTotalSeats(), event.getReservedSeats());

            // 3. 최근 1시간 판매량 조회
            int lastHourSales = rankingRepository.getLastHourSales(event.getConcertId());

            // 4. 랭킹 점수 계산
            FastSellingConcert fastSelling = FastSellingConcert.of(event.getConcertId(), event.getConcertTitle(), event.getTotalSeats(), event.getReservedSeats(), lastHourSales);

            // 5. Redis에 랭킹 점수 저장
            rankingRepository.updateConcertScore(event.getConcertId(), fastSelling.getRankingScore());

            log.info("콘서트 랭킹 업데이트 완료 - concertId={}, score={}, salesRate={}, salesSpeed={}",
                    event.getConcertId(), fastSelling.getRankingScore(), fastSelling.getSalesRate(), fastSelling.getSalesSpeed());

        } catch (Exception e) {
            log.error("랭킹 업데이트 실패 - concertId={}, error={}", event.getConcertId(), e.getMessage(), e);
            // 랭킹 업데이트 실패는 비즈니스 로직에 영향을 주지 않음
            // TODO: 랭킹 복구 기능 추후 추가
        }
    }
}
