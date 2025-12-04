package com.gomdol.concert.common.infra.idempotency.persistnence;

import com.gomdol.concert.common.application.idempotency.port.out.IdempotencyKeyRepository;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.common.domain.idempotency.model.IdempotencyKey;
import com.gomdol.concert.common.infra.idempotency.persistnence.entity.IdempotencyKeyEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepository {

    private final IdempotencyKeyJpaRepository jpaRepository;

    @Override
    public Optional<Long> findByIdempotencyKey(String key, String userId, ResourceType type) {
        return jpaRepository.findByIdempotencyKey(key, userId, type)
                .map(IdempotencyKeyEntity::getResourceId);
    }

    @Override
    public void save(IdempotencyKey key) {
        jpaRepository.save(IdempotencyKeyEntity.fromDomain(key));
    }
}
