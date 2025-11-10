package com.gomdol.concert.point.application.port.in;

import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;

// TODO: 현재는 충전/사용/환불을 하나로 통합함 추후 분리가 필요하면 그떄 분리
public interface SavePointPort {
    PointResponse savePoint(String userId, PointRequest req);
}
