package com.gomdol.concert.point.application.port.in;

import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.presentation.dto.PointRequest;

// TODO: 현재는 충전/사용/환불을 하나로 통합함 추후 분리가 필요하면 그떄 분리
public interface SavePointPort {
    PointSaveResponse savePoint(PointRequest req);
    record PointSaveResponse(Long historyId, String userId, Long balance) {
        public static PointSaveResponse fromDomain(Point point, Long historyId) {
            return new PointSaveResponse(historyId,point.getUserId(), point.getBalance());
        }
    }
}
