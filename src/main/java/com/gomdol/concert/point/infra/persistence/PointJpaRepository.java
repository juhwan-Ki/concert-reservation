package com.gomdol.concert.point.infra.persistence;

import com.gomdol.concert.point.infra.persistence.entity.PointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointJpaRepository extends JpaRepository<PointEntity, String> {
}
