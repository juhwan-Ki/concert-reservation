package com.gomdol.concert.queue.infra.persistence;

import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import com.gomdol.concert.queue.infra.persistence.entity.QueueTokenEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DBQueueRepository implements QueueRepository {

    private final QueueJpaRepository jpaRepository;
    private final static List<QueueStatus> queryStatus = List.of(QueueStatus.WAITING, QueueStatus.ENTERED);

    @Override
    @Transactional
    public QueueToken createToken(Long targetId, String userId, String token, long waitingTtlSeconds) {

        QueueTokenEntity activeToken = jpaRepository.findByTargetIdAndUserIdAndStatusIn(targetId, userId, queryStatus)
                .orElseGet(() -> {
                    QueueTokenEntity newEntity = QueueTokenEntity.createWaiting(targetId, userId, token, waitingTtlSeconds);
                    try {
                        return jpaRepository.saveAndFlush(newEntity); // UNIQUE 위반 시 아래 catch에서 재조회
                    } catch (DataIntegrityViolationException dup) {
                        // 동시 INSERT 경합 시 멱등: 기존 레코드 재조회
                        return jpaRepository.findByTargetIdAndUserIdAndStatusIn(targetId, userId, queryStatus)
                                .orElseThrow(() -> new RuntimeException("토큰을 찾을 수 없습니다"));
                    }
                });

        // position/total 계산
        long ahead = jpaRepository.countWaitingAhead(targetId, activeToken.getId(), Instant.now());
        long total = jpaRepository.countWaiting(targetId);
        long position = ahead + 1;

        return QueueToken.create(activeToken.getToken(), userId, targetId, QueueStatus.WAITING, position, total, activeToken.getExpiresAt());
    }

    @Override
    public Optional<QueueToken> findToken(Long targetId, String userId) {
        Optional<QueueTokenEntity> entity = jpaRepository.findByTargetIdAndUserIdAndStatusIn(targetId, userId, queryStatus);
        if(entity.isEmpty())
            return Optional.empty();

        QueueTokenEntity token = entity.get();
        // 대기중이 아니면 순번 의미 없음
        if (token.getStatus() == QueueStatus.ENTERED)
            return Optional.of(QueueToken.create(token.getToken(), userId, targetId, QueueStatus.ENTERED, 0L, 0L, token.getExpiresAt()));

        // EXPIRED 처리 (발급했지만 TTL 지난 경우)
        Instant now = Instant.now();
        if (token.getExpiresAt().isBefore(now) && token.getStatus() == QueueStatus.WAITING) {
            token.changeStatus(QueueStatus.EXPIRED);
            return Optional.of(QueueToken.create(token.getToken(), userId, targetId, QueueStatus.EXPIRED, 0L, 0L, token.getExpiresAt()));
        }

        long ahead = jpaRepository.countWaitingAhead(targetId, token.getId(), now);
        long total = jpaRepository.countWaiting(targetId);

        return Optional.of(QueueTokenEntity.toDomain(token, ahead, total));
    }

    @Override
    public Long rankInWaiting(Long targetId, String token) {
        return 0L;
    }

    @Override
    public Long waitingSize(Long targetId) {
        return 0L;
    }

    @Override
    public boolean isActive(Long targetId, String token) {
        return false;
    }

    @Override
    public List<String> pollNextWaiting(Long targetId, int n) {
        return List.of();
    }

    @Override
    public void activate(Long targetId, List<String> tokens, long activeTtlSec) {

    }

    @Override
    public void expireIfNeeded(Long targetId, String token) {

    }
}
