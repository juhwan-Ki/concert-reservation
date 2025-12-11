package com.gomdol.concert.concert.application.eventhandler;

import com.gomdol.concert.concert.application.service.FastSellingRankingService;
import com.gomdol.concert.payment.domain.event.PaymentCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

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
    @DisplayName("결제 완료 이벤트를 받아 랭킹 서비스를 호출한다")
    void handlePaymentCompleted() {
        // given
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 10L, 100L, 5L, "user123", 2, "아이유 콘서트", 1000, 500, LocalDateTime.now());
        // when
        eventHandler.handlePaymentCompleted(event);
        // then
        verify(rankingService, times(1)).handlePaymentCompleted(event);
    }

    @Test
    @DisplayName("랭킹 서비스에서 예외가 발생해도 이벤트 핸들러는 예외를 전파하지 않는다")
    void handlePaymentCompleted_ServiceException() {
        // given
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 10L, 100L, 5L, "user123", 2, "콘서트", 1000, 500, LocalDateTime.now());
        doThrow(new RuntimeException("Ranking service error")).when(rankingService).handlePaymentCompleted(any());

        // when & then - 예외가 전파되지 않아야 함
        eventHandler.handlePaymentCompleted(event);
        verify(rankingService, times(1)).handlePaymentCompleted(event);
    }

    @Test
    @DisplayName("여러 결제 완료 이벤트를 순차적으로 처리한다")
    void handleMultiplePaymentCompletedEvents() {
        // given
        PaymentCompletedEvent event1 = new PaymentCompletedEvent(1L, 10L, 100L, 5L, "user1", 2, "콘서트 A", 1000, 100, LocalDateTime.now());
        PaymentCompletedEvent event2 = new PaymentCompletedEvent(2L, 11L, 101L, 6L, "user2", 3, "콘서트 B", 2000, 500, LocalDateTime.now());

        // when
        eventHandler.handlePaymentCompleted(event1);
        eventHandler.handlePaymentCompleted(event2);

        // then
        verify(rankingService, times(1)).handlePaymentCompleted(event1);
        verify(rankingService, times(1)).handlePaymentCompleted(event2);
    }

    @Test
    @DisplayName("동일 콘서트의 여러 공연 결제 이벤트를 처리한다")
    void handleMultipleShowsFromSameConcert() {
        // given - 같은 concertId, 다른 showId
        PaymentCompletedEvent event1 = new PaymentCompletedEvent(1L, 10L, 100L, 5L, "user1", 2, "아이유 콘서트", 1000, 100, LocalDateTime.now());
        PaymentCompletedEvent event2 = new PaymentCompletedEvent(2L, 11L, 101L, 5L, "user2", 3, "아이유 콘서트", 1000, 200, LocalDateTime.now());

        // when
        eventHandler.handlePaymentCompleted(event1);
        eventHandler.handlePaymentCompleted(event2);

        // then
        verify(rankingService, times(2)).handlePaymentCompleted(any());
    }
}
