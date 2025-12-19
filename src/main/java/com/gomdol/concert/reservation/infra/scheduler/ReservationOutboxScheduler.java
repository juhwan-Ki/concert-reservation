package com.gomdol.concert.reservation.infra.scheduler;

import com.gomdol.concert.common.application.outbox.OutboxEventPublisher;
import com.gomdol.concert.common.application.outbox.OutboxRepository;
import com.gomdol.concert.common.domain.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reservation Service용 Outbox 스케줄러
 * Reservation Outbox 테이블의 이벤트를 Kafka로 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationOutboxScheduler {

    @Qualifier("reservationOutboxRepository")
    private final OutboxRepository outboxRepository;

    private final OutboxEventPublisher outboxEventPublisher;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void publishPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents();
            if (pendingEvents.isEmpty())
                return;
            log.info("[Reservation Outbox] PENDING 이벤트 발행 시작 - 개수: {}", pendingEvents.size());

            int published = 0;
            for (OutboxEvent event : pendingEvents) {
                try {
                    outboxEventPublisher.publishEvent(event);
                    published++;

                    if (published >= BATCH_SIZE)
                        break;
                } catch (Exception e) {
                    log.error("[Reservation Outbox] 이벤트 발행 실패 - eventId={}", event.getId(), e);
                }
            }

            log.info("[Reservation Outbox] 이벤트 발행 완료 - 발행 개수: {}/{}", published, pendingEvents.size());

        } catch (Exception e) {
            log.error("[Reservation Outbox] Scheduler 예외 발생", e);
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void retryFailedEvents() {
        try {
            List<OutboxEvent> failedEvents = outboxRepository.findFailedEventsForRetry(MAX_RETRIES);
            if (failedEvents.isEmpty())
                return;

            log.info("[Reservation Outbox] FAILED 이벤트 재시도 시작 - 개수: {}", failedEvents.size());
            int retried = 0;
            for (OutboxEvent event : failedEvents) {
                try {
                    outboxEventPublisher.publishEvent(event);
                    retried++;

                } catch (Exception e) {
                    log.error("[Reservation Outbox] 재시도 실패 - eventId={}, retryCount={}",
                            event.getId(), event.getRetryCount(), e);
                }
            }

            log.info("[Reservation Outbox] 재시도 완료 - 재시도 개수: {}/{}", retried, failedEvents.size());
        } catch (Exception e) {
            log.error("[Reservation Outbox] 재시도 Scheduler 예외 발생", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldPublishedEvents() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);
            List<OutboxEvent> oldEvents = outboxRepository.findOldPublishedEvents(threshold);
            if (oldEvents.isEmpty())
                return;

            log.info("[Reservation Outbox] 오래된 이벤트 정리 시작 - 대상 개수: {}", oldEvents.size());

            int deleted = 0;
            for (OutboxEvent event : oldEvents) {
                try {
                    outboxRepository.delete(event);
                    deleted++;

                } catch (Exception e) {
                    log.error("[Reservation Outbox] 이벤트 삭제 실패 - eventId={}", event.getId(), e);
                }
            }

            log.info("[Reservation Outbox] 이벤트 정리 완료 - 삭제 개수: {}/{}", deleted, oldEvents.size());
        } catch (Exception e) {
            log.error("[Reservation Outbox] 정리 Scheduler 예외 발생", e);
        }
    }
}
