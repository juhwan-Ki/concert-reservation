package com.gomdol.concert.point.application.usecase;

import com.gomdol.concert.point.application.port.in.GetPointBalancePort;
import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetPointBalanceUseCase implements GetPointBalancePort {

    private final PointRepository pointRepository;

    @Override
    public PointResponse getPoint(String userId) {
        log.info("userId: {}", userId);
        Point point = pointRepository.findByUserId(userId).orElseGet(() -> Point.create(userId, 0L)); // 포인트가 없으면 초기 값을 반환
        return PointResponse.fromDomain(point);
    }
}
