package com.gomdol.concert.venue.application.port.out;

import com.gomdol.concert.venue.domain.model.VenueSeat;

import java.util.List;

public interface VenueSeatRepository {
    List<VenueSeat> findByIds(List<Long> seatIds);
}
