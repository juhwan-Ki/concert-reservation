package com.gomdol.concert.show.infra.persistence.entity;

import com.gomdol.concert.common.infra.persistence.entity.SoftDeleteEntity;
import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import com.gomdol.concert.show.domain.model.ShowStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "shows",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_show_unique_slot",
                        columnNames = {"concert_id","show_at"})
        })
@SQLDelete(sql = "UPDATE shows SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ShowEntity extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★ 연관관계의 주인 (FK 보유)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_show_concert"))
    private ConcertEntity concert;

    @Column(nullable = false, name = "show_at")
    private LocalDateTime showAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 20)
    private ShowStatus status; // SCHEDULED, ON_SALE, SOLD_OUT, CANCELLED

    @Column(nullable = false)
    private int reservationCnt = 0;

    @Column(nullable = false)
    private int capacity;

    private ShowEntity(ConcertEntity concert, LocalDateTime showAt, ShowStatus status, int capacity) {
        this.concert = concert;
        this.showAt = showAt;
        this.status = status;
        this.capacity = capacity;
    }

    public static ShowEntity create(ConcertEntity concert, LocalDateTime showAt, ShowStatus status, int capacity) {
        return new ShowEntity(concert, showAt, status, capacity);
    }
}
