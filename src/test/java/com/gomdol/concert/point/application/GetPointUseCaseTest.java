package com.gomdol.concert.point.application;

import com.gomdol.concert.point.application.port.in.GetPointBalancePort.PointSearchResponse;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.application.usecase.GetPointBalanceUseCase;
import com.gomdol.concert.point.domain.model.Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetPointUseCaseTest {

    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    private GetPointBalanceUseCase getPointBalanceUseCase;

    @Test
    public void 포인트가_없으면_0원으로_초기화_후_반환한다() throws Exception {
        // given
        Point point = Point.create(FIXED_UUID,0L);
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        // when
        PointSearchResponse response = getPointBalanceUseCase.getPoint(FIXED_UUID);
        // then
        assertThat(response).isEqualTo(PointSearchResponse.fromDomain(point));
        assertThat(response.balance()).isEqualTo(0L);

        verify(pointRepository).findByUserIdWithLock(FIXED_UUID);
    }

    @Test
    public void 포인트가_존재하면_해당_포인트를_반환한다() throws Exception {
        Point point = Point.create(FIXED_UUID,10000L);
        when(pointRepository.findByUserIdWithLock(FIXED_UUID)).thenReturn(Optional.of(point));
        // when
        PointSearchResponse response = getPointBalanceUseCase.getPoint(FIXED_UUID);
        // then
        assertThat(response).isEqualTo(PointSearchResponse.fromDomain(point));
        assertThat(response.balance()).isEqualTo(10000L);

        verify(pointRepository).findByUserIdWithLock(FIXED_UUID);
    }
}
