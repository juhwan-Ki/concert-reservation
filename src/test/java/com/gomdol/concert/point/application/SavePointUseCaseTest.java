package com.gomdol.concert.point.application;

import com.gomdol.concert.common.application.idempotency.port.in.CreateIdempotencyKey;
import com.gomdol.concert.point.application.port.in.SavePointPort.PointSaveResponse;
import com.gomdol.concert.point.application.usecase.SavePointUseCase;
import com.gomdol.concert.point.domain.model.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SavePointUseCaseTest {

    @Mock
    private CreateIdempotencyKey createIdempotencyKey;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private SavePointUseCase savePointUseCase;

    @Test
    public void 충전_성공시_포인트_증가_후_이력이_저장된다() throws Exception {
        // given
        String requestId = UUID.randomUUID().toString();
        PointRequest req = new PointRequest(requestId, FIXED_UUID,10000L, UseType.CHARGE);
        Point point = Point.create(FIXED_UUID,0L);
        long beforeBalance = point.getBalance();
        long afterBalance = beforeBalance + req.amount();
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        // when
        PointSaveResponse response = savePointUseCase.savePoint(req);

        // then
        assertThat(response.balance()).isEqualTo(afterBalance);
        assertThat(response.userId()).isEqualTo(FIXED_UUID);

        ArgumentCaptor<PointHistory> histCap = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(histCap.capture());

        PointHistory saved = histCap.getValue();
        assertThat(saved.getRequestId()).isEqualTo(req.requestId());
        assertThat(saved.getAmount()).isEqualTo(req.amount());
        assertThat(saved.getUseType()).isEqualTo(req.useType());
        assertThat(saved.getBeforeBalance()).isEqualTo(beforeBalance);
        assertThat(saved.getAfterBalance()).isEqualTo(afterBalance);

        InOrder inOrder = inOrder(pointRepository, pointHistoryRepository);
        inOrder.verify(pointRepository).save(any(Point.class));
        inOrder.verify(pointHistoryRepository).save(any(PointHistory.class));
    }

    @Test
    public void 충전_중_이력저장_실패하면_예외를_던진다() throws Exception {
        // given
        String requestId = UUID.randomUUID().toString();
        PointRequest req = new PointRequest(requestId, FIXED_UUID,10000L, UseType.CHARGE);
        Point point = Point.create(FIXED_UUID,0L);

        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // pointHistory 저장 시점에 제약 위반/에러가 난 것처럼 예외 발생 유도
        when(pointHistoryRepository.save(any(PointHistory.class)))
                .thenThrow(new DataIntegrityViolationException("force history save failure"));

        // when & then
        assertThatThrownBy(() -> savePointUseCase.savePoint(req))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 호출 순서(여기까진 수행됨): 포인트 저장 → 이력 저장(예외)
        InOrder inOrder = inOrder(pointRepository, pointHistoryRepository);
        inOrder.verify(pointRepository).save(any(Point.class));
        inOrder.verify(pointHistoryRepository).save(any(PointHistory.class));

        // 이력 저장은 시도되었지만 실패(예외)로 끝남
        verifyNoMoreInteractions(pointRepository, pointHistoryRepository);
    }

    @Test
    public void 사용_성공시_잔액을_차감하고_이력이_저장된다() throws Exception {
        // given
        String requestId = UUID.randomUUID().toString();
        PointRequest req = new PointRequest(requestId, FIXED_UUID,3000L, UseType.USE);
        Point point = Point.create(FIXED_UUID,10000L);
        long beforeBalance = point.getBalance();
        long afterBalance = beforeBalance - req.amount();
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        // when
        PointSaveResponse response = savePointUseCase.savePoint(req);

        // then
        assertThat(response.balance()).isEqualTo(afterBalance);
        assertThat(response.userId()).isEqualTo(FIXED_UUID);

        ArgumentCaptor<PointHistory> histCap = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(histCap.capture());

        PointHistory saved = histCap.getValue();
        assertThat(saved.getRequestId()).isEqualTo(req.requestId());
        assertThat(saved.getAmount()).isEqualTo(-req.amount()); // 이력에서 사용값에는 -를 붙임
        assertThat(saved.getUseType()).isEqualTo(req.useType());
        assertThat(saved.getBeforeBalance()).isEqualTo(beforeBalance);
        assertThat(saved.getAfterBalance()).isEqualTo(afterBalance);

        InOrder inOrder = inOrder(pointRepository, pointHistoryRepository);
        inOrder.verify(pointRepository).save(any(Point.class));
        inOrder.verify(pointHistoryRepository).save(any(PointHistory.class));
    }

    @Test
    public void 사용_중_이력저장_실패하면_예외를_던진다() throws Exception {
        // given
        String requestId = UUID.randomUUID().toString();
        PointRequest req = new PointRequest(requestId, FIXED_UUID, 3000L, UseType.USE);
        Point point = Point.create(FIXED_UUID,10000L);

        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // pointHistory 저장 시점에 제약 위반/에러가 난 것처럼 예외 발생 유도
        when(pointHistoryRepository.save(any(PointHistory.class)))
                .thenThrow(new DataIntegrityViolationException("force history save failure"));

        // when & then
        assertThatThrownBy(() -> savePointUseCase.savePoint(req))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 호출 순서(여기까진 수행됨): 포인트 저장 → 이력 저장(예외)
        InOrder inOrder = inOrder(pointRepository, pointHistoryRepository);
        inOrder.verify(pointRepository).save(any(Point.class));
        inOrder.verify(pointHistoryRepository).save(any(PointHistory.class));

        // 이력 저장은 시도되었지만 실패(예외)로 끝남
        verifyNoMoreInteractions(pointRepository, pointHistoryRepository);
    }
}
