package com.gomdol.concert.common.infra.idempotency.persistnence;

import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.common.infra.idempotency.persistnence.entity.IdempotencyKeyEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM IdempotencyKeyEntity i WHERE i.idempotencyKey = :key AND i.userId = :userId AND i.resourceType = :type")
    Optional<IdempotencyKeyEntity> findByIdempotencyKey(@Param("key") String key, @Param("userId") String userId, @Param("type") ResourceType type);
}
