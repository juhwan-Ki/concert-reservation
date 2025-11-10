package com.gomdol.concert.point.application;

import com.gomdol.concert.point.application.usecase.SavePointUseCase;
import com.gomdol.concert.point.domain.model.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static com.gomdol.concert.common.FixedField.FIXED_REQUEST_ID;
import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SavePointUseCaseTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private SavePointUseCase savePointUseCase;

    @Test
    public void 충전_성공시_포인트_증가_후_이력이_저장된다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 10000L, UseType.CHARGE);
        Point point = Point.create(FIXED_UUID,0L);
        long beforeBalance = point.getBalance();
        long afterBalance = beforeBalance + req.amount();
        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        // when
        PointResponse response = savePointUseCase.savePoint(FIXED_UUID, req);

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
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // pointHistory 저장 시점에 제약 위반/에러가 난 것처럼 예외 발생 유도
        when(pointHistoryRepository.save(any(PointHistory.class)))
                .thenThrow(new DataIntegrityViolationException("force history save failure"));

        // when & then
        assertThatThrownBy(() -> savePointUseCase.savePoint(FIXED_UUID, req))
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
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        // when
        PointResponse response = savePointUseCase.savePoint(FIXED_UUID, req);

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
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // pointHistory 저장 시점에 제약 위반/에러가 난 것처럼 예외 발생 유도
        when(pointHistoryRepository.save(any(PointHistory.class)))
                .thenThrow(new DataIntegrityViolationException("force history save failure"));

        // when & then
        assertThatThrownBy(() -> savePointUseCase.savePoint(FIXED_UUID, req))
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
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // 히스토리 첫 저장은 정상
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        PointResponse first = savePointUseCase.savePoint(FIXED_UUID, req);
        assertThat(first.balance()).isEqualTo(10000L);

        // 2차 호출: 멱등키로 과거 히스토리가 조회됨 → 같은 응답 재사용
        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID))
                .thenReturn(Optional.of(PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, 10_000L, UseType.CHARGE, 0L, 10_000L)));

        PointResponse second = savePointUseCase.savePoint(FIXED_UUID, req);
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
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        // 히스토리 첫 저장은 정상
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        PointResponse first = savePointUseCase.savePoint(FIXED_UUID, req);
        assertThat(first.balance()).isEqualTo(7000L);

        // 2차 호출: 멱등키로 과거 히스토리가 조회됨 → 같은 응답 재사용
        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID))
                .thenReturn(Optional.of(PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, 3000L, UseType.USE, 10000L, 7000L)));

        PointResponse second = savePointUseCase.savePoint(FIXED_UUID, req);
        assertThat(second.balance()).isEqualTo(7000L);

        // 두 번째 호출에서는 추가 포인트 변경/저장이 일어나지 않아도 되도록 검증(선택)
        verify(pointRepository, times(1)).save(any(Point.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    // ========== 재시도 로직 테스트 ==========

    @Test
    public void PessimisticLockException_발생시_재시도_후_성공한다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 5000L, UseType.CHARGE);
        Point point = Point.create(FIXED_UUID, 10000L);

        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());

        // 첫 번째 시도: PessimisticLockException 발생
        // 두 번째 시도: 성공
        when(pointRepository.findByUserIdWithLock(FIXED_UUID))
                .thenThrow(new PessimisticLockException("Lock timeout"))
                .thenReturn(Optional.of(point));

        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        PointResponse response = savePointUseCase.savePoint(FIXED_UUID, req);

        // then
        assertThat(response.balance()).isEqualTo(15000L);
        assertThat(response.userId()).isEqualTo(FIXED_UUID);

        // 2번 조회 시도 (1번 실패, 1번 성공)
        verify(pointRepository, times(2)).findByUserIdWithLock(FIXED_UUID);
        // 1번 저장
        verify(pointRepository, times(1)).save(any(Point.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    public void LockTimeoutException_발생시_재시도_후_성공한다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 3000L, UseType.USE);
        Point point = Point.create(FIXED_UUID, 10000L);

        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());

        // 첫 번째, 두 번째 시도: LockTimeoutException 발생
        // 세 번째 시도: 성공
        when(pointRepository.findByUserIdWithLock(FIXED_UUID))
                .thenThrow(new LockTimeoutException("Lock wait timeout exceeded"))
                .thenThrow(new LockTimeoutException("Lock wait timeout exceeded"))
                .thenReturn(Optional.of(point));

        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        PointResponse response = savePointUseCase.savePoint(FIXED_UUID, req);

        // then
        assertThat(response.balance()).isEqualTo(7000L);
        assertThat(response.userId()).isEqualTo(FIXED_UUID);

        // 3번 조회 시도 (2번 실패, 1번 성공)
        verify(pointRepository, times(3)).findByUserIdWithLock(FIXED_UUID);
        verify(pointRepository, times(1)).save(any(Point.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    public void CannotAcquireLockException_발생시_재시도_후_성공한다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 2000L, UseType.CHARGE);
        Point point = Point.create(FIXED_UUID, 5000L);

        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());

        // 첫 번째 시도: CannotAcquireLockException
        // 두 번째 시도: 성공
        when(pointRepository.findByUserIdWithLock(FIXED_UUID))
                .thenThrow(new CannotAcquireLockException("Cannot acquire lock"))
                .thenReturn(Optional.of(point));

        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        PointResponse response = savePointUseCase.savePoint(FIXED_UUID, req);

        // then
        assertThat(response.balance()).isEqualTo(7000L);

        verify(pointRepository, times(2)).findByUserIdWithLock(FIXED_UUID);
        verify(pointRepository, times(1)).save(any(Point.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    public void 최대_재시도_횟수_초과시_예외를_던진다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 1000L, UseType.CHARGE);

        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());

        // 3번 모두 실패
        when(pointRepository.findByUserIdWithLock(FIXED_UUID))
                .thenThrow(new PessimisticLockException("Lock timeout"))
                .thenThrow(new PessimisticLockException("Lock timeout"))
                .thenThrow(new PessimisticLockException("Lock timeout"));

        // when & then
        assertThatThrownBy(() -> savePointUseCase.savePoint(FIXED_UUID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("처리 중인 요청이 너무 많습니다");

        // 3번 조회 시도 (모두 실패)
        verify(pointRepository, times(3)).findByUserIdWithLock(FIXED_UUID);
        // 저장까지 가지 못함
        verify(pointRepository, never()).save(any(Point.class));
        verify(pointHistoryRepository, never()).save(any(PointHistory.class));
    }

    @Test
    public void 첫_시도에서_성공하면_재시도하지_않는다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 8000L, UseType.CHARGE);
        Point point = Point.create(FIXED_UUID, 2000L);

        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());

        // 첫 번째 시도에서 바로 성공
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));

        when(pointRepository.save(any(Point.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pointHistoryRepository.save(any(PointHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        PointResponse response = savePointUseCase.savePoint(FIXED_UUID, req);

        // then
        assertThat(response.balance()).isEqualTo(10000L);

        // 1번만 조회 (재시도 없음)
        verify(pointRepository, times(1)).findByUserIdWithLock(FIXED_UUID);
        verify(pointRepository, times(1)).save(any(Point.class));
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    public void 락_예외와_일반_예외가_섞여도_재시도는_락_예외만_처리한다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 1000L, UseType.CHARGE);

        when(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, FIXED_REQUEST_ID)).thenReturn(Optional.empty());

        // IllegalArgumentException은 재시도 대상이 아니므로 바로 실패해야 함
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenThrow(new IllegalArgumentException("Invalid user"));

        // when & then
        assertThatThrownBy(() -> savePointUseCase.savePoint(FIXED_UUID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid user");

        // 재시도 없이 1번만 시도
        verify(pointRepository, times(1)).findByUserIdWithLock(FIXED_UUID);
        verify(pointRepository, never()).save(any(Point.class));
    }
}
