package com.gomdol.concert.concert.infra.persistence;

import com.gomdol.concert.concert.domain.ConcertStatus;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConcertQueryJpaRepository extends JpaRepository<ConcertEntity, Long> {
    @Query("""
       select c
       from ConcertEntity c
       join c.venue v
       where c.status = :status
         and ( lower(c.title)  like concat('%', lower(:keyword), '%')
               or lower(c.artist) like concat('%', lower(:keyword), '%')
               or lower(v.name)   like concat('%', lower(:keyword), '%') )
    """)
    Page<ConcertEntity> findAllPublicAndKeyWord(Pageable pageable, @Param("keyword") String keyword, @Param("status") ConcertStatus status);

    @Query("select c from ConcertEntity c where c.status = 'PUBLIC'")
    Page<ConcertEntity> findAllPublic(Pageable pageable);

    @Query("""
        select distinct c
        from ConcertEntity c
        join fetch c.venue v
        left join fetch c.shows s
        where c.id = :id
          and c.status = :status
    """)
    Optional<ConcertEntity> findPublicDetailById(@Param("id") Long id, @Param("status") ConcertStatus status);
}
