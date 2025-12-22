package com.gomdol.concert.reservation.infra.kafka.producer;

import com.gomdol.concert.reservation.domain.event.ReservationCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;

/**
 * 예약 완료 이벤트를 Kafka로 발행하는 Producer
 * 트랜잭션 커밋 후 Kafka 토픽으로 이벤트 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventProducer {

    private final KafkaTemplate<String, ReservationCompletedEvent> kafkaTemplate;

    @Value("${kafka.topics.reservation-completed}")
    private String topic;

    /**
     * 예약 완료 이벤트를 Kafka로 발행
     * - 트랜잭션 커밋 후 실행 (AFTER_COMMIT)
     * - 비동기로 Kafka에 전송
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishReservationCompleted(ReservationCompletedEvent event) {
        log.info("Kafka로 예약 완료 이벤트 발행 시작 - reservationId: {}, userId: {}",
                event.getReservationId(), event.getUserId());

        try {
            // 파티션 키: reservationId (같은 예약은 같은 파티션으로 전송)
            String key = event.getReservationId().toString();

            CompletableFuture<SendResult<String, ReservationCompletedEvent>> future = kafkaTemplate.send(topic, key, event);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Kafka 전송 성공 - topic: {}, partition: {}, offset: {}, reservationId: {}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.getReservationId());
                } else {
                    log.error("Kafka 전송 실패 - reservationId: {}", event.getReservationId(), ex);
                    // TODO: 실패 시 재시도 or 별도 저장소에 저장
                }
            });

        } catch (Exception e) {
            log.error("Kafka 전송 중 예외 발생 - reservationId: {}", event.getReservationId(), e);
            // TODO: 실패한 이벤트를 DB에 저장하여 나중에 재발행
        }
    }
}
