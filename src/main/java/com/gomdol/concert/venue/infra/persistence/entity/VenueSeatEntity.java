package com.gomdol.concert.venue.infra.persistence.entity;

import com.gomdol.concert.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "venue_seats")
public class VenueSeatEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String seatLabel;

    @Column(nullable = false, length = 10)
    private String rowLabel;

    @Column(nullable = false)
    private int setNumber;

    @Column(nullable = false)
    private long price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private VenueEntity venue;
}
