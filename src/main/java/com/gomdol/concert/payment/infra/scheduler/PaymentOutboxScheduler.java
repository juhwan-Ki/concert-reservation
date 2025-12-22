package com.gomdol.concert.payment.infra.scheduler;

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
 * Payment Service용 Outbox 스케줄러
 * Payment Outbox 테이블의 이벤트를 Kafka로 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {

    @Qualifier("paymentOutboxRepositoryImpl")
    private final OutboxRepository outboxRepository;

    private final OutboxEventPublisher outboxEventPublisher;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 100;

    /**
     * PENDING 상태 이벤트를 Kafka로 발행
     * 매 5초마다 실행
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void publishPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents();
            if (pendingEvents.isEmpty())
                return;

            log.info("[Payment Outbox] PENDING 이벤트 발행 시작 - 개수: {}", pendingEvents.size());

            int published = 0;
            for (OutboxEvent event : pendingEvents) {
                try {
                    outboxEventPublisher.publishEvent(event);
                    published++;

                    if (published >= BATCH_SIZE) {
                        log.info("[Payment Outbox] 배치 크기 도달 - 다음 스케줄에서 나머지 처리");
                        break;
                    }

                } catch (Exception e) {
                    log.error("[Payment Outbox] 이벤트 발행 실패 - eventId={}", event.getId(), e);
                }
            }

            log.info("[Payment Outbox] 이벤트 발행 완료 - 발행 개수: {}/{}", published, pendingEvents.size());

        } catch (Exception e) {
            log.error("[Payment Outbox] Scheduler 예외 발생", e);
        }
    }

    /**
     * FAILED 상태 이벤트 재시도
     * 매 1분마다 실행
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void retryFailedEvents() {
        try {
            List<OutboxEvent> failedEvents = outboxRepository.findFailedEventsForRetry(MAX_RETRIES);
            if (failedEvents.isEmpty())
                return;

            log.info("[Payment Outbox] FAILED 이벤트 재시도 시작 - 개수: {}", failedEvents.size());

            int retried = 0;
            for (OutboxEvent event : failedEvents) {
                try {
                    outboxEventPublisher.publishEvent(event);
                    retried++;

                } catch (Exception e) {
                    log.error("[Payment Outbox] 재시도 실패 - eventId={}, retryCount={}",
                            event.getId(), event.getRetryCount(), e);
                }
            }

            log.info("[Payment Outbox] 재시도 완료 - 재시도 개수: {}/{}", retried, failedEvents.size());
        } catch (Exception e) {
            log.error("[Payment Outbox] 재시도 Scheduler 예외 발생", e);
        }
    }

    /**
     * 오래된 PUBLISHED 이벤트 정리
     * 매일 새벽 3시 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldPublishedEvents() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);
            List<OutboxEvent> oldEvents = outboxRepository.findOldPublishedEvents(threshold);

            if (oldEvents.isEmpty()) {
                log.info("[Payment Outbox] 정리할 오래된 이벤트 없음");
                return;
            }
            log.info("[Payment Outbox] 오래된 이벤트 정리 시작 - 대상 개수: {}", oldEvents.size());

            int deleted = 0;
            for (OutboxEvent event : oldEvents) {
                try {
                    outboxRepository.delete(event);
                    deleted++;

                } catch (Exception e) {
                    log.error("[Payment Outbox] 이벤트 삭제 실패 - eventId={}", event.getId(), e);
                }
            }
            log.info("[Payment Outbox] 이벤트 정리 완료 - 삭제 개수: {}/{}", deleted, oldEvents.size());
        } catch (Exception e) {
            log.error("[Payment Outbox] 정리 Scheduler 예외 발생", e);
        }
    }
}
