package com.gomdol.concert.reservation.infra.persistence.entity;

import com.gomdol.concert.common.domain.BaseEntity;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "reservations")
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class ReservationEntity extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_code", nullable = false, unique = true)
    private String reservationCode;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // 1:N 관계 매핑 (CASCADE로 함께 저장/삭제)
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ReservationSeatEntity> reservationSeats = new ArrayList<>();

    public void addReservationSeat(ReservationSeatEntity reservationSeat) {
        reservationSeats.add(reservationSeat);
        reservationSeat.addReservation(this);
    }

    public static Reservation toDomain(ReservationEntity entity) {
        // JPA 연관관계가 있어도 도메인에서는 ID만 사용
        List<ReservationSeat> domainSeats = entity.getReservationSeats()
                .stream()
                .map(ReservationSeatEntity::toDomain)
                .toList();

        return Reservation.of(
                entity.getId(),
                entity.getUserId(),
                entity.getReservationCode(),
                domainSeats,
                entity.getAmount(),
                entity.getExpiresAt(),
                entity.getConfirmedAt()
        );
    }

    public static ReservationEntity fromDomain(Reservation reservation) {
        ReservationEntity entity = ReservationEntity.builder()
                .id(reservation.getId())
                .userId(reservation.getUserId())
                .reservationCode(reservation.getReservationCode())
                .amount(reservation.getAmount())
                .expiresAt(reservation.getExpiresAt())
                .confirmedAt(reservation.getConfirmedAt())
                .build();

        reservation.getReservationSeats().forEach(domainSeat -> {
            ReservationSeatEntity seatEntity = ReservationSeatEntity.fromDomain(domainSeat, entity);
            entity.addReservationSeat(seatEntity); // 양방향 연관관계 설정
        });

        return entity;
    }
}
