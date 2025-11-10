package com.gomdol.concert.point.application.service;

import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.domain.event.PointRequestedEvent;
import com.gomdol.concert.point.domain.event.PointResponseEvent;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.domain.model.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.gomdol.concert.point.domain.policy.PointPolicy.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventHandler {

    private final PointRepository pointRepository;
    private final PointHistoryRepository historyRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 포인트 사용 처리
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePointUseRequested(PointRequestedEvent event) {
        try {
            Point point = pointRepository.findByUserIdWithLock(event.userId())
                    .orElseGet(() -> Point.create(event.userId(), 0L));

            if(event.useType() != UseType.USE)
                throw new IllegalArgumentException("사용 타입이 맞지 않습니다. type:" + event.useType());

            long amount = event.amount();

            validateAmount(amount);
            validateUse(amount);
            if(point.getBalance() - amount < 0)
                throw new IllegalArgumentException("포인트가 부족합니다");

            long before = point.getBalance();
            point.usePoint(event.amount());
            long after = point.getBalance();

            pointRepository.save(point);

            PointHistory history = PointHistory.create(
                    event.userId(),
                    event.requestId(),
                    amount,
                    event.useType(),
                    before,
                    after);
            historyRepository.save(history);
            log.info("포인트 사용 히스토리 저장: userId={}, amount={}, before={}, after={}", event.userId(), event.amount(), before, after);

            // 성공 이벤트 발행
            eventPublisher.publishEvent(PointResponseEvent.succeededEvent(
                    event.userId(),
                    event.requestId(),
                    event.amount()));

        } catch (Exception e) {
            // 실패 이벤트 발행
            eventPublisher.publishEvent(PointResponseEvent.failedEvent(
                    event.userId(),
                    event.requestId(),
                    e.getMessage(),
                    event.amount())
            );
        }
    }

    // 포인트 충전 처리
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePointChargeRequested(PointRequestedEvent event) {
        try {
            Point point = pointRepository.findByUserIdWithLock(event.userId())
                    .orElseGet(() -> Point.create(event.userId(), 0L));

            if(event.useType() != UseType.CHARGE)
                throw new IllegalArgumentException("사용 타입이 맞지 않습니다. type:" + event.useType());

            long amount = event.amount();
            validateAmount(amount);
            validateCharge(amount);

            long before = point.getBalance();
            point.changeBalance(event.amount());
            long after = point.getBalance();
            pointRepository.save(point);

            PointHistory history = PointHistory.create(
                    event.userId(),
                    event.requestId(),
                    amount,
                    event.useType(),
                    before,
                    after);
            historyRepository.save(history);
            log.info("포인트 충전 히스토리 저장: userId={}, amount={}, before={}, after={}", event.userId(), event.amount(), before, after);

            // 성공 이벤트 발행
            eventPublisher.publishEvent(PointResponseEvent.succeededEvent(
                    event.userId(),
                    event.requestId(),
                    event.amount()));

        } catch (Exception e) {
            // 실패 이벤트 발행
            eventPublisher.publishEvent(PointResponseEvent.failedEvent(
                    event.userId(),
                    event.requestId(),
                    e.getMessage(),
                    event.amount())
            );
        }
    }
}
