package com.gomdol.concert.venue.infra.persistence;

import com.gomdol.concert.venue.application.port.out.VenueSeatRepository;
import com.gomdol.concert.venue.domain.model.VenueSeat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VenueSeatRepositoryImpl implements VenueSeatRepository {

    private final VenueSeatJpaRepository jpaRepository;

    @Override
    public List<VenueSeat> findByIds(List<Long> seatIds) {
        return List.of();
    }
}
