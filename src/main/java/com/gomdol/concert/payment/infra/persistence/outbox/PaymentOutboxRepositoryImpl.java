package com.gomdol.concert.payment.infra.persistence.outbox;

import com.gomdol.concert.common.application.outbox.OutboxRepository;
import com.gomdol.concert.common.domain.outbox.OutboxEvent;
import com.gomdol.concert.common.domain.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Repository
@RequiredArgsConstructor
public class PaymentOutboxRepositoryImpl implements OutboxRepository {

    private final PaymentOutboxJpaRepository jpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return jpaRepository.save(PaymentOutboxEntity.fromDomain(event)).toDomain();
    }

    @Override
    public List<OutboxEvent> findPendingEvents() {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                .stream()
                .map(PaymentOutboxEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OutboxEvent> findFailedEventsForRetry(int maxRetries) {
        return jpaRepository.findFailedEventsForRetry(maxRetries)
                .stream()
                .map(PaymentOutboxEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OutboxEvent> findOldPublishedEvents(LocalDateTime threshold) {
        return jpaRepository.findOldPublishedEvents(threshold)
                .stream()
                .map(PaymentOutboxEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(OutboxEvent event) {
        jpaRepository.deleteById(event.getId());
    }
}
