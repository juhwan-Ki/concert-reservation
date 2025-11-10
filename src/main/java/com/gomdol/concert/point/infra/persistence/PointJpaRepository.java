package com.gomdol.concert.point.infra.persistence;

import com.gomdol.concert.point.infra.persistence.entity.PointEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<PointEntity, String> {
    // 비관적락 적용 (5초 타임아웃)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")  // 5초 타임아웃
    })
    @Query("SELECT p FROM PointEntity p WHERE p.userId = :userId")
    Optional<PointEntity> findByUserIdWithLock(@Param("userId") String userId);
}
