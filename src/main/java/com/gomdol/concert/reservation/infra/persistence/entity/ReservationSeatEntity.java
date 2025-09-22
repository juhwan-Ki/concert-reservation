package com.gomdol.concert.reservation.infra.persistence.entity;

import com.gomdol.concert.common.domain.CreateEntity;
import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "reservation_seats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reservation_seats_unique_slot",
                        columnNames = {"show_id", "seat_id"})
        })
@Getter
@Builder
public class ReservationSeatEntity extends CreateEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private ReservationEntity reservation;

    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "show_id")
    private Long showId;

    @Column(name = "price")
    private Long price;

    @Enumerated(EnumType.STRING)
    private ReservationSeatStatus status;

    public void addReservation(ReservationEntity reservation) {
        this.reservation = reservation;
    }

    public static ReservationSeat toDomain(ReservationSeatEntity entity) {
        return ReservationSeat.of(
                entity.getId(),
                entity.getReservation().getId(),
                entity.getSeatId(),
                entity.getShowId(),
                entity.getStatus()
        );
    }

    public static ReservationSeatEntity fromDomain(ReservationSeat reservationSeat, ReservationEntity reservation) {
        return ReservationSeatEntity.builder()
                .id(reservationSeat.getId())
                .reservation(reservation)
                .seatId(reservationSeat.getSeatId())
                .showId(reservationSeat.getShowId())
                .status(reservationSeat.getStatus())
                .build();
    }
}
