package com.gomdol.concert.concert.application.service;

import com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository;
import com.gomdol.concert.concert.domain.model.FastSellingConcert;
import com.gomdol.concert.payment.domain.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * 결제 완료 이벤트 처리
     * - 이벤트에서 받은 정보로 Redis 업데이트
     * - 시간당 판매량 증가
     * - 랭킹 점수 계산 및 업데이트
     *
     * @param event 결제 완료 이벤트 (공연 정보 포함)
     */
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.debug("결제 완료 이벤트 처리 (랭킹 업데이트) - paymentId={}, concertId={}, title={}", event.paymentId(), event.concertId(), event.concertTitle());
            // 1. 시간당 판매량 증가 (콘서트 단위)
            rankingRepository.incrementHourlySales(event.concertId());
            // 2. Redis에 콘서트 통계 저장 (이벤트 정보 사용)
            rankingRepository.saveConcertStats(event.concertId(), event.concertTitle(), event.totalSeats(), event.reservedSeats());
            // 3. 최근 1시간 판매량 조회
            int lastHourSales = rankingRepository.getLastHourSales(event.concertId());
            // 4. 랭킹 점수 계산
            FastSellingConcert fastSelling = FastSellingConcert.of(event.concertId(), event.concertTitle(), event.totalSeats(), event.reservedSeats(), lastHourSales);
            // 5. Redis에 랭킹 점수 저장
            rankingRepository.updateConcertScore(event.concertId(), fastSelling.getRankingScore());
            log.debug("콘서트 랭킹 업데이트 완료 - concertId={}, score={}, salesRate={}, salesSpeed={}",
                    event.concertId(), fastSelling.getRankingScore(), fastSelling.getSalesRate(), fastSelling.getSalesSpeed());

        } catch (Exception e) {
            log.error("결제 완료 이벤트 처리 실패 (랭킹 업데이트) - concertId={}", event.concertId(), e);
            // 랭킹 업데이트 실패는 비즈니스 로직에 영향을 주지 않음
        }
    }
}
