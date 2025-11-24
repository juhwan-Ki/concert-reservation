package com.gomdol.concert.point.infra.persistence.entity;

import com.gomdol.concert.common.infra.persistence.entity.BaseEntity;
import com.gomdol.concert.point.domain.model.Point;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "points")
public class PointEntity extends BaseEntity {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId; // users.id와 동일 값 (PK = FK)


    @Column(name = "balance", nullable = false)
    private long balance;

    private PointEntity(String userId, long balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public static Point toDomain(PointEntity entity) {
        return Point.create(entity.userId, entity.balance);
    }

    public static PointEntity fromDomain(Point point) {
        return new PointEntity(point.getUserId(),point.getBalance());
    }

    public void updateBalance(long newBalance) {
        this.balance = newBalance;
    }
}
