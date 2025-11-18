package com.gomdol.concert.point.application.facade;

import com.gomdol.concert.point.application.usecase.SavePointUseCase;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import static com.gomdol.concert.common.FixedField.FIXED_REQUEST_ID;
import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointFacade 재시도 로직 테스트")
public class PointFacadeTest {

    @Mock
    private SavePointUseCase savePointUseCase;

    @InjectMocks
    private PointFacade pointFacade;

    @Test
    public void PessimisticLockException_발생시_재시도_후_성공한다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 5000L, UseType.CHARGE);
        PointResponse expectedResponse = new PointResponse(FIXED_UUID, 15000L);

        // 첫 번째 시도: PessimisticLockException 발생
        // 두 번째 시도: 성공
        when(savePointUseCase.savePoint(FIXED_UUID, req))
                .thenThrow(new PessimisticLockException("Lock timeout"))
                .thenReturn(expectedResponse);

        // when
        PointResponse response = pointFacade.savePoint(FIXED_UUID, req);

        // then
        assertThat(response.balance()).isEqualTo(15000L);
        assertThat(response.userId()).isEqualTo(FIXED_UUID);

        // 2번 호출 (1번 실패, 1번 성공)
        verify(savePointUseCase, times(2)).savePoint(FIXED_UUID, req);
    }

    @Test
    public void LockTimeoutException_발생시_재시도_후_성공한다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 3000L, UseType.USE);
        PointResponse expectedResponse = new PointResponse(FIXED_UUID, 7000L);

        // 첫 번째, 두 번째 시도: LockTimeoutException 발생
        // 세 번째 시도: 성공
        when(savePointUseCase.savePoint(FIXED_UUID, req))
                .thenThrow(new LockTimeoutException("Lock wait timeout exceeded"))
                .thenThrow(new LockTimeoutException("Lock wait timeout exceeded"))
                .thenReturn(expectedResponse);

        // when
        PointResponse response = pointFacade.savePoint(FIXED_UUID, req);

        // then
        assertThat(response.balance()).isEqualTo(7000L);
        assertThat(response.userId()).isEqualTo(FIXED_UUID);

        // 3번 호출 (2번 실패, 1번 성공)
        verify(savePointUseCase, times(3)).savePoint(FIXED_UUID, req);
    }

    @Test
    public void CannotAcquireLockException_발생시_재시도_후_성공한다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 2000L, UseType.CHARGE);
        PointResponse expectedResponse = new PointResponse(FIXED_UUID, 7000L);

        // 첫 번째 시도: CannotAcquireLockException
        // 두 번째 시도: 성공
        when(savePointUseCase.savePoint(FIXED_UUID, req))
                .thenThrow(new CannotAcquireLockException("Cannot acquire lock"))
                .thenReturn(expectedResponse);

        // when
        PointResponse response = pointFacade.savePoint(FIXED_UUID, req);

        // then
        assertThat(response.balance()).isEqualTo(7000L);

        // 2번 호출 (1번 실패, 1번 성공)
        verify(savePointUseCase, times(2)).savePoint(FIXED_UUID, req);
    }

    @Test
    public void 최대_재시도_횟수_초과시_예외를_던진다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 1000L, UseType.CHARGE);

        // 3번 모두 실패
        when(savePointUseCase.savePoint(FIXED_UUID, req))
                .thenThrow(new PessimisticLockException("Lock timeout"))
                .thenThrow(new PessimisticLockException("Lock timeout"))
                .thenThrow(new PessimisticLockException("Lock timeout"));

        // when & then
        assertThatThrownBy(() -> pointFacade.savePoint(FIXED_UUID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("처리 중인 요청이 너무 많습니다");

        // 3번 호출 (모두 실패)
        verify(savePointUseCase, times(3)).savePoint(FIXED_UUID, req);
    }

    @Test
    public void 첫_시도에서_성공하면_재시도하지_않는다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 8000L, UseType.CHARGE);
        PointResponse expectedResponse = new PointResponse(FIXED_UUID, 10000L);

        // 첫 번째 시도에서 바로 성공
        when(savePointUseCase.savePoint(FIXED_UUID, req)).thenReturn(expectedResponse);

        // when
        PointResponse response = pointFacade.savePoint(FIXED_UUID, req);

        // then
        assertThat(response.balance()).isEqualTo(10000L);

        // 1번만 호출 (재시도 없음)
        verify(savePointUseCase, times(1)).savePoint(FIXED_UUID, req);
    }

    @Test
    public void 락_예외와_일반_예외가_섞여도_재시도는_락_예외만_처리한다() throws Exception {
        // given
        PointRequest req = new PointRequest(FIXED_REQUEST_ID, 1000L, UseType.CHARGE);

        // IllegalArgumentException은 재시도 대상이 아니므로 바로 실패해야 함
        when(savePointUseCase.savePoint(FIXED_UUID, req))
                .thenThrow(new IllegalArgumentException("Invalid user"));

        // when & then
        assertThatThrownBy(() -> pointFacade.savePoint(FIXED_UUID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid user");

        // 재시도 없이 1번만 호출
        verify(savePointUseCase, times(1)).savePoint(FIXED_UUID, req);
    }
}