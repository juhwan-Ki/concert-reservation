package com.gomdol.concert.queue.infra.persistence;

import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import com.gomdol.concert.queue.infra.persistence.entity.QueueTokenEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    public QueueToken issueToken(Long targetId, String userId, String token, QueueStatus status, long waitingTtlSeconds) {

        QueueTokenEntity activeToken = jpaRepository.findByTargetIdAndUserIdAndStatusIn(targetId, userId, queryStatus)
                .orElseGet(() -> {
                    QueueTokenEntity newEntity = QueueTokenEntity.create(targetId, userId, token, status, waitingTtlSeconds);
                    try {
                        return jpaRepository.saveAndFlush(newEntity); // UNIQUE 위반 시 아래 catch에서 재조회
                    } catch (DataIntegrityViolationException dup) {
                        // 동시 INSERT 경합 시 멱등: 기존 레코드 재조회
                        return jpaRepository.findByTargetIdAndUserIdAndStatusIn(targetId, userId, queryStatus)
                                .orElseThrow(() -> new RuntimeException("토큰을 찾을 수 없습니다"));
                    }
                });

        if(status == QueueStatus.WAITING) {
            long position = jpaRepository.countWaitingAhead(targetId, activeToken.getId(), Instant.now()) + 1;
            return QueueTokenEntity.toDomainWithPosition(activeToken, position);
        }

        return QueueTokenEntity.toDomain(activeToken);
    }

    @Override
    public QueueToken findQueuePositionByTargetIdAndUserId(Long targetId, String userId) {
        QueueTokenEntity entity = QueueTokenEntity.create(targetId, userId, null, QueueStatus.WAITING, 0L);
        // position 계산
        long ahead = jpaRepository.countWaitingAhead(targetId, entity.getId(), Instant.now());
        long position  = ahead + 1;
        return QueueTokenEntity.toDomainWithPosition(entity, position);
    }

    @Override
    public boolean isWaiting(Long targetId) {
        return jpaRepository.countByTargetIdAndStatus(targetId, QueueStatus.WAITING) > 0;
    }

    @Override
    public Optional<QueueToken> findByTargetIdAndUserId(Long targetId, String userId) {
        Optional<QueueTokenEntity> entity = jpaRepository.findByTargetIdAndUserIdAndStatusIn(targetId, userId, queryStatus);
        if(entity.isEmpty())
            return Optional.empty();

        QueueTokenEntity token = entity.get();
        // 대기중이 아니면 순번 의미 없음
        if(token.getStatus() == QueueStatus.ENTERED) {
            long ttl = Math.max(0, Duration.between(Instant.now(), token.getExpiresAt()).toSeconds());
            return Optional.of(QueueToken.create(token.getToken(), token.getUserId(), targetId, QueueStatus.ENTERED, 0L, ttl));
        }

        // EXPIRED 처리 (발급했지만 TTL 지난 경우)
        Instant now = Instant.now();
        if(token.getExpiresAt().isBefore(now) && token.getStatus() == QueueStatus.WAITING) {
            token.changeStatus(QueueStatus.EXPIRED);
            return Optional.of(QueueToken.create(token.getToken(), userId, targetId, QueueStatus.EXPIRED, 0L, 0L));
        }

        long position = jpaRepository.countWaitingAhead(targetId, token.getId(), now) + 1;
        return Optional.of(QueueTokenEntity.toDomainWithPosition(token, position));
    }

    @Override
    public Optional<QueueToken> findByTargetIdAndToken(Long targetId, String token) {
        Optional<QueueTokenEntity> entity = jpaRepository.findByTokenAndTargetId(token, targetId);
        if(entity.isEmpty())
            return Optional.empty();

        QueueTokenEntity tokenEntity = entity.get();
        // 대기중이 아니면 순번 의미 없음
        if(tokenEntity.getStatus() == QueueStatus.ENTERED) {
            long ttl = Math.max(0, Duration.between(Instant.now(), tokenEntity.getExpiresAt()).toSeconds());
            return Optional.of(QueueToken.create(tokenEntity.getToken(), tokenEntity.getUserId(), targetId, QueueStatus.ENTERED, 0L, ttl));
        }

        // EXPIRED 처리 (발급했지만 TTL 지난 경우)
        Instant now = Instant.now();
        if(tokenEntity.getExpiresAt().isBefore(now) && tokenEntity.getStatus() == QueueStatus.WAITING) {
            tokenEntity.changeStatus(QueueStatus.EXPIRED);
            return Optional.of(QueueToken.create(tokenEntity.getToken(), tokenEntity.getUserId(), targetId, QueueStatus.EXPIRED, 0L, 0L));
        }

        long position = jpaRepository.countWaitingAhead(targetId, tokenEntity.getId(), now) + 1;
        return Optional.of(QueueTokenEntity.toDomainWithPosition(tokenEntity, position));
    }

    @Override
    public List<Long> findActiveTargetIds(Instant now) {
        return jpaRepository.findActiveTargetIds(now);
    }

    @Override
    public long countEnteredActive(Long targetId, Instant now) {
        return jpaRepository.countEnteredActive(targetId, now);
    }

    @Override
    public void save(QueueToken token) {
        jpaRepository.save(QueueTokenEntity.fromDomain(token));
    }

    @Override
    public List<QueueToken> findAllTargetIdAndStatusAndOffsetLimit(Long targetId, QueueStatus queueStatus, Instant now, int limit) {
        List<QueueTokenEntity> entities = jpaRepository.findAllTargetIdAndStatusAndOffsetLimit(targetId, queueStatus, now, limit);
        if(entities.isEmpty())
            return List.of();
        return entities.stream().map(QueueTokenEntity::toDomain).toList();
    }

    @Override
    public List<QueueToken> findAllExpiredAndOffsetLimit(Instant now, int limit) {
        List<QueueTokenEntity> entities = jpaRepository.findAllExpiredAndOffsetLimit(queryStatus, now, limit);
        if(entities.isEmpty())
            return List.of();
        return entities.stream().map(QueueTokenEntity::toDomain).toList();
    }
}
