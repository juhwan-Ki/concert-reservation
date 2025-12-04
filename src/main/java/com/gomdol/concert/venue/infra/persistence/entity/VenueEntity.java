package com.gomdol.concert.venue.infra.persistence.entity;

import com.gomdol.concert.common.infra.persistence.entity.SoftDeleteEntity;
import com.gomdol.concert.venue.domain.model.Venue;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "venues")
@SQLDelete(sql = "UPDATE venues SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class VenueEntity extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private int capacity;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<VenueSeatEntity> venueSeats = new ArrayList<>();

    private VenueEntity(String name, String address, int capacity) {
        this.id = null;
        this.name = name;
        this.address = address;
        this.capacity = capacity;
    }

    public static VenueEntity create(String name, String address, int capacity) {
        return new VenueEntity(name, address, capacity);
    }

    public static Venue toDomain(VenueEntity entity) {
        return Venue.create(entity.getId(), entity.getName(), entity.getAddress(), entity.getCapacity());
    }
}
