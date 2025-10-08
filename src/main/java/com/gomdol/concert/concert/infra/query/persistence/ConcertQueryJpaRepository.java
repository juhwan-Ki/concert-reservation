package com.gomdol.concert.concert.infra.query.persistence;

import com.gomdol.concert.concert.domain.model.ConcertStatus;
import com.gomdol.concert.concert.infra.command.persistence.ConcertEntity;
import com.gomdol.concert.concert.infra.query.projection.ConcertDetailProjection;
import com.gomdol.concert.show.infra.query.projection.ShowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConcertQueryJpaRepository extends JpaRepository<ConcertEntity, Long> {

    @Query("""
        select
          c.id, c.title, v.name as venueName, c.artist,
          c.ageRating, c.runningTime, c.description,
          c.posterUrl, c.startAt, c.endAt, c.deletedAt
        from ConcertEntity c
        join VenueEntity v on v.id = c.venue.id
        where c.id = :id and c.status = :status
        and c.deletedAt is null
    """)
    Optional<ConcertDetailProjection> findByIdAndStatus(@Param("id") Long id, @Param("status") ConcertStatus status);

    @Query("""
        select s.id as id, s.showAt, s.status
        from ShowEntity s
        where s.concert.id = :concertId
        and s.deletedAt is null
        order by s.showAt asc
    """)
    List<ShowProjection> findShowsByConcertId(@Param("concertId") Long id);

    @Query("""
       select c
       from ConcertEntity c
       join c.venue v
       where c.status = :status
         and ( lower(c.title)  like concat('%', lower(:keyword), '%')
               or lower(c.artist) like concat('%', lower(:keyword), '%')
               or lower(v.name)   like concat('%', lower(:keyword), '%') )
    """)
    Page<ConcertEntity> findAllByStatusAndKeyWord(Pageable pageable, @Param("keyword") String keyword, @Param("status") ConcertStatus status);

    @Query("select c from ConcertEntity c where c.status = :status")
    Page<ConcertEntity> findAllByStatus(Pageable pageable, @Param("status") ConcertStatus status);
}
