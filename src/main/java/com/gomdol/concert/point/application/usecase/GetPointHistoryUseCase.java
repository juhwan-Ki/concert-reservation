package com.gomdol.concert.point.application.usecase;

import com.gomdol.concert.point.application.port.in.GetPointHistoryPort;
import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.domain.model.PointHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetPointHistoryUseCase implements GetPointHistoryPort {

    private final PointHistoryRepository pointHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<PointHistoryResponse> getPointHistory(Long historyId) {
        log.info("historyId: {}", historyId);
        PointHistory pointHistory = pointHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 내역입니다."));
        return Optional.of(PointHistoryResponse.fromDomain(pointHistory));
    }
}
