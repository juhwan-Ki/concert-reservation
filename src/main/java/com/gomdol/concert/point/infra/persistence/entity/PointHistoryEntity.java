package com.gomdol.concert.point.infra.persistence.entity;

import com.gomdol.concert.common.domain.CreateEntity;
import com.gomdol.concert.point.domain.history.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "point_history",
        indexes = {
                @Index(name = "ix_history_user_created", columnList = "user_id, created_at DESC")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_point_history_user_req", columnNames = {"user_id", "request_id"})
        })
@Getter
public class PointHistoryEntity extends CreateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: history에서 user로 조회를 역조회를 하지 않을 것 같아서 일단 userId만 둠
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "request_id", length = 64, nullable = false)
    private String requestId; // 멱등키

    @Column(name = "amount", nullable = false)
    private long amount; // +충전/환불, -사용

    @Enumerated(EnumType.STRING)
    @Column(name = "use_type", nullable = false, length = 20)
    private UseType useType;

    @Column(name = "before_balance", nullable = false)
    private long beforeBalance;

    @Column(name = "after_balance", nullable = false)
    private long afterBalance;

    private PointHistoryEntity(String userId, String requestId, long amount, UseType useType, long beforeBalance, long afterBalance) {
        this.userId = userId;
        this.requestId = requestId;
        this.amount = amount;
        this.useType = useType;
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
    }

    public static PointHistoryEntity fromDomain(PointHistory pointHistory) {
        return new PointHistoryEntity(pointHistory.getUserId(), pointHistory.getRequestId(), pointHistory.getAmount(), pointHistory.getUseType(), pointHistory.getBeforeBalance(), pointHistory.getAfterBalance());
    }

    public static PointHistory toDomain(PointHistoryEntity entity) {
        return new PointHistory(entity.id, entity.userId, entity.requestId, entity.amount, entity.useType, entity.beforeBalance, entity.afterBalance);
    }
}
