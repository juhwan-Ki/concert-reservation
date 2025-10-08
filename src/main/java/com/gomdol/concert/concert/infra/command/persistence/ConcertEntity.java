package com.gomdol.concert.concert.infra.command.persistence;

import com.gomdol.concert.common.domain.SoftDeleteEntity;
import com.gomdol.concert.concert.domain.model.AgeRating;
import com.gomdol.concert.concert.domain.model.Concert;
import com.gomdol.concert.concert.domain.model.ConcertStatus;
import com.gomdol.concert.show.infra.command.persistence.ShowEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "concerts")
@SQLDelete(sql = "UPDATE concerts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ConcertEntity extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 100)
    private String artist;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "running_time")
    private int runningTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, name = "age_rating")
    private AgeRating ageRating;

    @Column(length = 100, name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(length = 100, name = "poster_url")
    private String posterUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConcertStatus status;

    @Column(nullable = false, name = "start_at")
    private LocalDate startAt;

    @Column(nullable = false, name = "end_at")
    private LocalDate endAt;

    @OneToMany(mappedBy = "concerts", fetch = FetchType.LAZY)
    @OrderBy("showAt ASC") // 날짜 오름차순
    private List<ShowEntity> shows = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_concert_venue"))
    private VenueEntity venue;

    public static ConcertEntity create(String title, String artist, String description, int runningTime, AgeRating ageRating,
             String thumbnailUrl, String posterUrl, ConcertStatus status, LocalDate startAt, LocalDate endAt)
    {
        return ConcertEntity.builder()
                .title(title)
                .artist(artist)
                .description(description)
                .runningTime(runningTime)
                .ageRating(ageRating)
                .thumbnailUrl(thumbnailUrl)
                .posterUrl(posterUrl)
                .status(status)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    public static Concert toDomain(ConcertEntity entity) {
        return Concert.create(entity.getId(), entity.getTitle(), entity.getArtist(), entity.getDescription(),
                entity.getRunningTime(), entity.getAgeRating(), entity.getThumbnailUrl(),
                entity.getPosterUrl(), entity.getStatus(), entity.getStartAt(), entity.getEndAt());
    }
}
