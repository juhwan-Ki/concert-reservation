package com.gomdol.concert.point.application;

import com.gomdol.concert.point.application.service.PointService;
import com.gomdol.concert.point.domain.history.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.domain.point.Point;
import com.gomdol.concert.point.domain.repository.PointHistoryRepository;
import com.gomdol.concert.point.domain.repository.PointRepository;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointService pointService;

    private static final String FIXED_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String FIXED_REQUEST_ID = "524910ab692b43c5b97ebadf176416cb7bf06da44f974b3bad33aca0778cebf7";

    @Test
    public void 포인트가_없으면_0원으로_초기화_후_반환한다() throws Exception {
        // given
        Point point = Point.create(FIXED_UUID,0L);
        when(pointRepository.findByUserId(FIXED_UUID)).thenReturn(Optional.of(point));
        // when
        PointResponse response = pointService.getPoint(FIXED_UUID);
        // then
        assertThat(response).isEqualTo(PointResponse.fromDomain(point));
        assertThat(response.balance()).isEqualTo(0L);

        verify(pointRepository).findByUserId(FIXED_UUID);
    }

    @Test
    public void 포인트가_존재하면_해당_포인트를_반환한다() throws Exception {
        Point point = Point.create(FIXED_UUID,10000L);
        when(pointRepository.findByUserId(FIXED_UUID)).thenReturn(Optional.of(point));
        // when
        PointResponse response = pointService.getPoint(FIXED_UUID);
        // then
        assertThat(response).isEqualTo(PointResponse.fromDomain(point));
        assertThat(response.balance()).isEqualTo(10000L);

        verify(pointRepository).findByUserId(FIXED_UUID);
    }

    @Test
    public void 충전_성공시_포인트_증가_후_이력이_저장된다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 10000L, UseType.CHARGE);
        Point point = Point.create(FIXED_UUID,0L);
        long beforeBalance = point.getBalance();
        long afterBalance = beforeBalance + req.amount();
        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());
        when(pointRepository.findByUserId(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        // when
        PointResponse response = pointService.savePoint(FIXED_UUID, req);

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
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 10000L, UseType.CHARGE);
        Point point = Point.create(FIXED_UUID,0L);

        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());
        when(pointRepository.findByUserId(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // pointHistory 저장 시점에 제약 위반/에러가 난 것처럼 예외 발생 유도
        when(pointHistoryRepository.save(any(PointHistory.class)))
                .thenThrow(new DataIntegrityViolationException("force history save failure"));

        // when & then
        assertThatThrownBy(() -> pointService.savePoint(FIXED_UUID, req))
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
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 3000L, UseType.USE);
        Point point = Point.create(FIXED_UUID,10000L);
        long beforeBalance = point.getBalance();
        long afterBalance = beforeBalance - req.amount();
        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());
        when(pointRepository.findByUserId(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        // when
        PointResponse response = pointService.savePoint(FIXED_UUID, req);

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
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 3000L, UseType.USE);
        Point point = Point.create(FIXED_UUID,10000L);

        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());
        when(pointRepository.findByUserId(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // pointHistory 저장 시점에 제약 위반/에러가 난 것처럼 예외 발생 유도
        when(pointHistoryRepository.save(any(PointHistory.class)))
                .thenThrow(new DataIntegrityViolationException("force history save failure"));

        // when & then
        assertThatThrownBy(() -> pointService.savePoint(FIXED_UUID, req))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 호출 순서(여기까진 수행됨): 포인트 저장 → 이력 저장(예외)
        InOrder inOrder = inOrder(pointRepository, pointHistoryRepository);
        inOrder.verify(pointRepository).save(any(Point.class));
        inOrder.verify(pointHistoryRepository).save(any(PointHistory.class));

        // 이력 저장은 시도되었지만 실패(예외)로 끝남
        verifyNoMoreInteractions(pointRepository, pointHistoryRepository);
    }

    @Test
    public void 같은_멱등키로_두번_충전해도_한번만_반영되고_같은_응답을_반환한다() throws Exception {
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 10000L, UseType.CHARGE);

        // 1차 호출: 기존 멱등기록 없음 → 정상 처리 & 히스토리 저장
        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID))
                .thenReturn(Optional.empty()); // 첫 호출 시

        Point point = Point.create(FIXED_UUID, 0L);
        when(pointRepository.findByUserId(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // 히스토리 첫 저장은 정상
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        PointResponse first = pointService.savePoint(FIXED_UUID, req);
        assertThat(first.balance()).isEqualTo(10000L);

        // 2차 호출: 멱등키로 과거 히스토리가 조회됨 → 같은 응답 재사용
        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID))
                .thenReturn(Optional.of(PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, 10_000L, UseType.CHARGE, 0L, 10_000L)));

        PointResponse second = pointService.savePoint(FIXED_UUID, req);
        assertThat(second.balance()).isEqualTo(10000L);

        // 두 번째 호출에서는 추가 포인트 변경/저장이 일어나지 않아도 되도록 검증(선택)
        verify(pointRepository, times(1)).save(any(Point.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    public void 같은_멱등키로_두번_사용해도_중복_차감되지_않고_같은_응답을_반환한다() throws Exception {
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 3000L, UseType.USE);

        // 1차 호출: 기존 멱등기록 없음 → 정상 처리 & 히스토리 저장
        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID))
                .thenReturn(Optional.empty()); // 첫 호출 시

        Point point = Point.create(FIXED_UUID, 10000L);
        when(pointRepository.findByUserId(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // 히스토리 첫 저장은 정상
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        PointResponse first = pointService.savePoint(FIXED_UUID, req);
        assertThat(first.balance()).isEqualTo(7000L);

        // 2차 호출: 멱등키로 과거 히스토리가 조회됨 → 같은 응답 재사용
        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID))
                .thenReturn(Optional.of(PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, 3000L, UseType.USE, 10000L, 7000L)));

        PointResponse second = pointService.savePoint(FIXED_UUID, req);
        assertThat(second.balance()).isEqualTo(7000L);

        // 두 번째 호출에서는 추가 포인트 변경/저장이 일어나지 않아도 되도록 검증(선택)
        verify(pointRepository, times(1)).save(any(Point.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }
}
