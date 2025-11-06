package com.gomdol.concert.point.application.port.in;

import com.gomdol.concert.point.presentation.dto.PointResponse;

public interface GetPointBalancePort {
    PointResponse getPoint(String userId);
}
