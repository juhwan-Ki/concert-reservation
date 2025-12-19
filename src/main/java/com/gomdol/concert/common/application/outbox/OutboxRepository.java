package com.gomdol.concert.common.application.outbox;

import com.gomdol.concert.common.domain.outbox.OutboxEvent;
import com.gomdol.concert.common.domain.outbox.OutboxStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox Repository Port
 */
public interface OutboxRepository {

    /**
     * Outbox 이벤트 저장
     */
    OutboxEvent save(OutboxEvent event);

    /**
     * PENDING 상태 이벤트 조회
     */
    List<OutboxEvent> findPendingEvents();

    /**
     * FAILED 상태이면서 재시도 가능한 이벤트 조회
     */
    List<OutboxEvent> findFailedEventsForRetry(int maxRetries);

    /**
     * 오래된 PUBLISHED 이벤트 조회 (정리용)
     */
    List<OutboxEvent> findOldPublishedEvents(LocalDateTime threshold);

    /**
     * 이벤트 삭제
     */
    void delete(OutboxEvent event);
}
