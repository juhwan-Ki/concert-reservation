package com.gomdol.concert.concert.application.service;

import com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository;
import com.gomdol.concert.concert.domain.event.RankingUpdateRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FastSellingRankingService 테스트
 */
@ExtendWith(MockitoExtension.class)
class FastSellingRankingServiceTest {

    @Mock
    private FastSellingRankingRepository rankingRepository;

    @InjectMocks
    private FastSellingRankingService rankingService;

    @Test
    @DisplayName("결제 완료 이벤트를 처리하여 콘서트 랭킹을 업데이트한다")
    void handlePaymentCompleted() {
        // given
        
        RankingUpdateRequestedEvent event = RankingUpdateRequestedEvent.of(1L, 5L, "아이유 콘서트 2025", 1000, 2, 500);

        when(rankingRepository.getLastHourSales(5L)).thenReturn(50);

        // when
        rankingService.rankingUpdate(event);

        // then
        verify(rankingRepository, times(1)).incrementHourlySales(5L);
        verify(rankingRepository, times(1)).saveConcertStats(5L, "아이유 콘서트 2025", 1000, 500);
        verify(rankingRepository, times(1)).getLastHourSales(5L);
        verify(rankingRepository, times(1)).updateConcertScore(eq(5L), anyDouble());
    }

    @Test
    @DisplayName("콘서트 랭킹 점수를 정확하게 계산하여 저장한다")
    void calculateAndSaveRankingScore() {
        // given
        RankingUpdateRequestedEvent event = RankingUpdateRequestedEvent.of(1L, 5L, "QWER 콘서트", 1000, 500, 2);
        when(rankingRepository.getLastHourSales(5L)).thenReturn(20);

        // when
        rankingService.rankingUpdate(event);

        // then
        // 판매율 50% + 판매속도(20) * 10 = 250
        verify(rankingRepository, times(1)).updateConcertScore(5L, 250.0);
    }

    @Test
    @DisplayName("판매율이 높을수록 더 높은 점수로 업데이트된다")
    void higherSalesRateResultsInHigherScore() {
        // given
        RankingUpdateRequestedEvent event = RankingUpdateRequestedEvent.of(1L, 5L, "인기 콘서트", 1000, 800, 2);
        when(rankingRepository.getLastHourSales(5L)).thenReturn(10);

        // when
        rankingService.rankingUpdate(event);

        // then
        // 판매율 80% + 판매속도(10) * 10 = 180
        verify(rankingRepository, times(1)).updateConcertScore(5L, 180.0);
    }

    @Test
    @DisplayName("판매속도가 빠를수록 더 높은 점수로 업데이트된다")
    void higherSalesSpeedResultsInHigherScore() {
        // given
        RankingUpdateRequestedEvent event = RankingUpdateRequestedEvent.of(1L, 5L, "빠른 콘서트", 1000, 300, 2);
        when(rankingRepository.getLastHourSales(5L)).thenReturn(100);

        // when
        rankingService.rankingUpdate(event);

        // then
        // 판매율 30% + 판매속도(100) * 10 = 1030
        verify(rankingRepository, times(1)).updateConcertScore(5L, 1030.0);
    }

    @Test
    @DisplayName("Repository 예외가 발생해도 서비스는 예외를 전파하지 않는다")
    void handleRepositoryException() {
        // given
        RankingUpdateRequestedEvent event = RankingUpdateRequestedEvent.of(1L, 5L, "콘서트", 1000, 500, 2);
        doThrow(new RuntimeException("Redis connection failed")).when(rankingRepository).incrementHourlySales(anyLong());

        // when & then - 예외가 전파되지 않아야 함
        rankingService.rankingUpdate(event);

        // 다른 메소드는 호출되지 않음
        verify(rankingRepository, never()).saveConcertStats(anyLong(), anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("여러 결제 이벤트를 순차적으로 처리할 수 있다")
    void handleMultiplePaymentEvents() {
        // given
        RankingUpdateRequestedEvent event1 = RankingUpdateRequestedEvent.of(1L, 5L, "콘서트 A", 1000, 100, 2);
        RankingUpdateRequestedEvent event2 = RankingUpdateRequestedEvent.of(2L, 5L, "콘서트 A", 1000, 200, 3);
        when(rankingRepository.getLastHourSales(5L))
                .thenReturn(10)  // 첫 번째 호출
                .thenReturn(20); // 두 번째 호출

        // when
        rankingService.rankingUpdate(event1);
        rankingService.rankingUpdate(event2);

        // then
        verify(rankingRepository, times(2)).incrementHourlySales(5L);
        verify(rankingRepository, times(2)).getLastHourSales(5L);
        verify(rankingRepository, times(2)).updateConcertScore(eq(5L), anyDouble());
    }

    @Test
    @DisplayName("전체 좌석이 0이어도 안전하게 처리한다")
    void handleZeroTotalSeats() {
        // given
        RankingUpdateRequestedEvent event = RankingUpdateRequestedEvent.of(1L, 5L, "특별 콘서트", 0, 0, 2);
        when(rankingRepository.getLastHourSales(5L)).thenReturn(5);

        // when
        rankingService.rankingUpdate(event);

        // then
        // 판매율 0% + 판매속도(5) * 10 = 50
        verify(rankingRepository, times(1)).updateConcertScore(5L, 50.0);
    }
}
