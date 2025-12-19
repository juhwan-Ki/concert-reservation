package com.gomdol.concert.common.application.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gomdol.concert.common.domain.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 이벤트를 Kafka로 발행하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Outbox 이벤트를 저장
     * 트랜잭션 내에서 호출되어야 함
     */
    public void saveOutboxEvent( String aggregateType,  String aggregateId,  String eventType,  String topic,  Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.create(aggregateType, aggregateId, eventType, topic, payload);
            outboxRepository.save(outboxEvent);
            log.debug("Outbox 이벤트 저장 - aggregateType={}, aggregateId={}, eventType={}", aggregateType, aggregateId, eventType);
        } catch (JsonProcessingException e) {
            log.error("Outbox 이벤트 직렬화 실패", e);
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }

    /**
     * Outbox 이벤트를 Kafka로 발행
     * Scheduler에서 호출됨
     */
    @Transactional
    public void publishEvent(OutboxEvent event) {
        try {
            // JSON을 Object로 역직렬화 (Kafka JsonSerializer가 처리)
            Object payload = objectMapper.readValue(event.getPayload(), Object.class);
            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Outbox Kafka 전송 성공 - topic={}, aggregateId={}, offset={}", event.getTopic(), event.getAggregateId(), result.getRecordMetadata().offset());
                            // 발행 성공 처리
                            OutboxEvent published = event.markAsPublished();
                            outboxRepository.save(published);
                        } else {
                            log.error("Outbox Kafka 전송 실패 - topic={}, aggregateId={}", event.getTopic(), event.getAggregateId(), ex);
                            // 발행 실패 처리
                            OutboxEvent failed = event.markAsFailed(ex.getMessage());
                            outboxRepository.save(failed);
                        }
                    });
        } catch (Exception e) {
            log.error("Outbox 이벤트 발행 중 예외 - eventId={}", event.getId(), e);
            // 예외 발생 시 실패 처리
            OutboxEvent failed = event.markAsFailed(e.getMessage());
            outboxRepository.save(failed);
        }
    }
}
