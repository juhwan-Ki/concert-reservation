package com.gomdol.concert.concert.application.eventhandler;

import com.gomdol.concert.concert.application.service.FastSellingRankingService;
import com.gomdol.concert.concert.domain.event.RankingUpdateRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * FastSellingEventHandler 테스트
 */
@ExtendWith(MockitoExtension.class)
class FastSellingEventHandlerTest {

    @Mock
    private FastSellingRankingService rankingService;

    @InjectMocks
    private FastSellingEventHandler eventHandler;

    @Test
    @DisplayName("랭킹 업데이트 요청 이벤트를 받아 서비스를 호출한다")
    void handleRankingUpdateRequested() {
        // given
        RankingUpdateRequestedEvent event = RankingUpdateRequestedEvent.of(1L, 5L, "테스트 콘서트", 1000, 500, 2);

        // when
        eventHandler.handleRankingUpdateRequested(event);

        // then
        verify(rankingService, times(1)).rankingUpdate(event);
    }

    @Test
    @DisplayName("랭킹 서비스에서 예외가 발생해도 이벤트 핸들러는 예외를 전파하지 않는다")
    void handleRankingUpdateRequested_ServiceException() {
        // given
        RankingUpdateRequestedEvent event = RankingUpdateRequestedEvent.of(1L, 5L, "테스트 콘서트", 1000, 500, 2);
        doThrow(new RuntimeException("Ranking service error")).when(rankingService).rankingUpdate(any());

        // when & then - 예외가 전파되지 않아야 함
        eventHandler.handleRankingUpdateRequested(event);
        verify(rankingService, times(1)).rankingUpdate(event);
    }

    @Test
    @DisplayName("여러 랭킹 업데이트 요청 이벤트를 순차적으로 처리한다")
    void handleMultipleRankingUpdateRequested() {
        // given
        RankingUpdateRequestedEvent event1 = RankingUpdateRequestedEvent.of(1L, 5L, "콘서트 A", 1000, 100, 2);
        RankingUpdateRequestedEvent event2 = RankingUpdateRequestedEvent.of(2L, 6L, "콘서트 B", 2000, 200, 3);

        // when
        eventHandler.handleRankingUpdateRequested(event1);
        eventHandler.handleRankingUpdateRequested(event2);

        // then
        verify(rankingService, times(1)).rankingUpdate(event1);
        verify(rankingService, times(1)).rankingUpdate(event2);
    }

    @Test
    @DisplayName("동일 콘서트의 여러 공연에 대한 랭킹 업데이트를 처리한다")
    void handleMultipleShowsFromSameConcert() {
        // given - 같은 concertId
        RankingUpdateRequestedEvent event1 = RankingUpdateRequestedEvent.of(1L, 5L, "아이유 콘서트", 1000, 100, 2);
        RankingUpdateRequestedEvent event2 = RankingUpdateRequestedEvent.of(2L, 5L, "아이유 콘서트", 1000, 200, 3);

        // when
        eventHandler.handleRankingUpdateRequested(event1);
        eventHandler.handleRankingUpdateRequested(event2);

        // then
        verify(rankingService, times(2)).rankingUpdate(any());
    }
}
