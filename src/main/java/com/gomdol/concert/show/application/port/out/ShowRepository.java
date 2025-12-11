package com.gomdol.concert.show.application.port.out;

import com.gomdol.concert.show.domain.model.Show;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShowRepository {
    Optional<Show> findById(Long id);
    List<Show> findByConcertId(Long concertId);

    /**
     * 여러 공연 조회
     * @param ids 공연 ID 목록
     * @return 공연 목록
     */
    List<Show> findByIds(List<Long> ids);

    /**
     * 활성 공연 ID 조회
     * @param now 현재 시간
     * @return 활성 공연 ID 목록
     */
    List<Long> findActiveShowIds(LocalDateTime now);
}
