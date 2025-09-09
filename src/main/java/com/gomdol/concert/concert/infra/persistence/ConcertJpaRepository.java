package com.gomdol.concert.concert.infra.persistence;

import com.gomdol.concert.concert.domain.ConcertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConcertJpaRepository extends JpaRepository<ConcertEntity, Long> {
    // 관리자용
    Page<ConcertEntity> findAllByStatus(ConcertStatus status, Pageable pageable);
    // 사용자용(public + keyword)
    @Query("""
       select c from ConcertEntity c
       where c.status = 'PUBLIC'
         and ( :keyword is null 
               or trim(:keyword) = '' 
               or c.title like concat('%', :keyword, '%') 
               or c.artist like concat('%', :keyword, '%') )
    """)
    Page<ConcertEntity> findAllPublicAndKeyWord(Pageable pageable, @Param("keyword") String keyword);

    // 사용자용(public)
    @Query("select c from ConcertEntity c where c.status = 'PUBLIC'")
    Page<ConcertEntity> findAllPublic(Pageable pageable);
}
