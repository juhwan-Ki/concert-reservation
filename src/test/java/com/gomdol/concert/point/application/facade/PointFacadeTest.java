package com.gomdol.concert.point.application.facade;

import com.gomdol.concert.common.application.cache.port.out.CacheRepository;
import com.gomdol.concert.common.application.idempotency.port.in.GetIdempotencyKey;
import com.gomdol.concert.common.application.lock.port.out.DistributedLock;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.point.application.port.in.GetPointHistoryPort;
import com.gomdol.concert.point.application.port.in.SavePointPort.PointSaveResponse;
import com.gomdol.concert.point.application.usecase.SavePointUseCase;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointFacade 테스트")
class PointFacadeTest {

    @Mock
    private CacheRepository cacheRepository;

    @Mock
    private GetIdempotencyKey getIdempotencyKey;

    @Mock
    private DistributedLock distributedLock;

    @Mock
    private SavePointUseCase savePointUseCase;

    @Mock
    private GetPointHistoryPort getPointHistoryPort;

    @InjectMocks
    private PointFacade pointFacade;

    @Test
    @DisplayName("Redis 캐시에 결과가 있으면 즉시 반환한다")
    void redis_캐시_히트_시_즉시_반환() {
        // Given
        String requestId = UUID.randomUUID().toString();
        PointRequest request = new PointRequest(requestId, FIXED_UUID, 5000L, UseType.CHARGE);
        PointSaveResponse cachedResponse = new PointSaveResponse(1L, FIXED_UUID, 15000L);

        when(cacheRepository.get(eq("point:result:" + requestId), eq(PointSaveResponse.class)))
                .thenReturn(Optional.of(cachedResponse));

        // When
        PointSaveResponse result = pointFacade.savePoint(request);

        // Then
        assertThat(result).isEqualTo(cachedResponse);
        assertThat(result.balance()).isEqualTo(15000L);

        // 캐시 히트 시 DB 조회나 락 획득 없이 바로 반환
        verify(cacheRepository).get(anyString(), eq(PointSaveResponse.class));
        verify(getIdempotencyKey, never()).getIdempotencyKey(anyString(), anyString(), any());
        verify(distributedLock, never()).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PointSaveResponse>>any());
    }

    @Test
    @DisplayName("DB 멱등키가 존재하면 기존 이력을 조회하여 반환한다")
    void DB_멱등키_존재_시_기존_이력_반환() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long historyId = 100L;
        PointRequest request = new PointRequest(requestId, FIXED_UUID, 3000L, UseType.USE);

        when(cacheRepository.get(anyString(), eq(PointSaveResponse.class))).thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT)).thenReturn(Optional.of(historyId));

        GetPointHistoryPort.PointHistoryResponse historyResponse =
                new GetPointHistoryPort.PointHistoryResponse(
                        historyId, FIXED_UUID, "USE", 3000L, 10000L, 7000L, LocalDateTime.now());
        when(getPointHistoryPort.getPointHistory(historyId)).thenReturn(Optional.of(historyResponse));

        // When
        PointSaveResponse result = pointFacade.savePoint(request);

        // Then
        assertThat(result.historyId()).isEqualTo(historyId);
        assertThat(result.userId()).isEqualTo(FIXED_UUID);
        assertThat(result.balance()).isEqualTo(7000L);

        // 멱등키가 있으면 락 획득 없이 바로 반환
        verify(getIdempotencyKey).getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT);
        verify(getPointHistoryPort).getPointHistory(historyId);
        verify(cacheRepository).set(anyString(), any(PointSaveResponse.class), any(Duration.class));
        verify(distributedLock, never()).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PointSaveResponse>>any());
    }

    @Test
    @DisplayName("정상 처리: 포인트 충전 후 캐시 및 멱등키 저장")
    void 정상_처리_포인트_충전() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long historyId = 200L;
        PointRequest request = new PointRequest(requestId, FIXED_UUID, 10000L, UseType.CHARGE);
        PointSaveResponse expectedResponse = new PointSaveResponse(historyId, FIXED_UUID, 20000L);

        when(cacheRepository.get(anyString(), eq(PointSaveResponse.class))).thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT)).thenReturn(Optional.empty());

        // DistributedLock mock: 콜백을 즉시 실행
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PointSaveResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PointSaveResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(savePointUseCase.savePoint(request)).thenReturn(expectedResponse);

        // When
        PointSaveResponse result = pointFacade.savePoint(request);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.balance()).isEqualTo(20000L);
        assertThat(result.historyId()).isEqualTo(historyId);

        // 정상 처리 후 캐시 저장 확인
        verify(savePointUseCase).savePoint(request);
        verify(cacheRepository).set(eq("point:result:" + requestId), eq(expectedResponse), any(Duration.class));
    }

    @Test
    @DisplayName("정상 처리: 포인트 사용 후 캐시 및 멱등키 저장")
    void 정상_처리_포인트_사용() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long historyId = 300L;
        PointRequest request = new PointRequest(requestId, FIXED_UUID, 5000L, UseType.USE);
        PointSaveResponse expectedResponse = new PointSaveResponse(historyId, FIXED_UUID, 5000L);

        when(cacheRepository.get(anyString(), eq(PointSaveResponse.class))).thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT)).thenReturn(Optional.empty());

        // DistributedLock mock: 콜백을 즉시 실행
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PointSaveResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PointSaveResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(savePointUseCase.savePoint(request)).thenReturn(expectedResponse);

        // When
        PointSaveResponse result = pointFacade.savePoint(request);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.balance()).isEqualTo(5000L);

        verify(savePointUseCase).savePoint(request);
        verify(cacheRepository).set(anyString(), any(PointSaveResponse.class), any(Duration.class));
    }

    @Test
    @DisplayName("DB 제약조건 위반 시 멱등키로 재조회하여 반환")
    void DB_제약조건_위반_시_멱등키_재조회() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long historyId = 400L;
        PointRequest request = new PointRequest(requestId, FIXED_UUID, 7000L, UseType.CHARGE);

        when(cacheRepository.get(anyString(), eq(PointSaveResponse.class))).thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT))
                .thenReturn(Optional.empty()) // 첫 조회는 없음
                .thenReturn(Optional.of(historyId)); // 재조회 시 존재

        // DistributedLock mock: 콜백을 즉시 실행
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PointSaveResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PointSaveResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        // SavePointUseCase가 DataIntegrityViolationException 발생
        when(savePointUseCase.savePoint(request)).thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        GetPointHistoryPort.PointHistoryResponse historyResponse =
                new GetPointHistoryPort.PointHistoryResponse(
                        historyId, FIXED_UUID, "CHARGE", 7000L, 10000L, 17000L, LocalDateTime.now());
        when(getPointHistoryPort.getPointHistory(historyId)).thenReturn(Optional.of(historyResponse));

        // When
        PointSaveResponse result = pointFacade.savePoint(request);

        // Then
        assertThat(result.historyId()).isEqualTo(historyId);
        assertThat(result.balance()).isEqualTo(17000L);

        // 제약조건 위반 시 멱등키로 재조회
        verify(savePointUseCase).savePoint(request);
        verify(getIdempotencyKey, times(2)).getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT);
        verify(getPointHistoryPort).getPointHistory(historyId);
        verify(cacheRepository).set(anyString(), any(PointSaveResponse.class), any(Duration.class));
    }

    @Test
    @DisplayName("DB 제약조건 위반이지만 멱등키가 없으면 예외 발생")
    void DB_제약조건_위반_but_멱등키_없으면_예외() {
        // Given
        String requestId = UUID.randomUUID().toString();
        PointRequest request = new PointRequest(requestId, FIXED_UUID, 5000L, UseType.USE);

        when(cacheRepository.get(anyString(), eq(PointSaveResponse.class))).thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT)).thenReturn(Optional.empty()); // 처음에도 없고

        // DistributedLock mock: 콜백을 즉시 실행
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PointSaveResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PointSaveResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        DataIntegrityViolationException originalException = new DataIntegrityViolationException("Other constraint");
        when(savePointUseCase.savePoint(request)).thenThrow(originalException);

        // 재조회해도 멱등키가 없음
        when(getIdempotencyKey.getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pointFacade.savePoint(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("포인트 처리 중 제약조건 위반")
                .hasCause(originalException);

        verify(savePointUseCase).savePoint(request);
        verify(getIdempotencyKey, times(2)).getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT);
    }

    @Test
    @DisplayName("같은 멱등키로 두 번 호출 시 중복 차감 없이 같은 응답 반환")
    void 멱등성_보장_중복_차감_방지() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long historyId = 500L;
        PointRequest request = new PointRequest(requestId, FIXED_UUID, 3000L, UseType.USE);
        PointSaveResponse firstResponse = new PointSaveResponse(historyId, FIXED_UUID, 7000L);

        // 첫 번째 호출
        when(cacheRepository.get(eq("point:result:" + requestId), eq(PointSaveResponse.class)))
                .thenReturn(Optional.empty()); // 첫 호출엔 캐시 없음
        when(getIdempotencyKey.getIdempotencyKey(requestId, FIXED_UUID, ResourceType.POINT))
                .thenReturn(Optional.empty());
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PointSaveResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PointSaveResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });
        when(savePointUseCase.savePoint(request)).thenReturn(firstResponse);

        // When: 첫 번째 호출
        PointSaveResponse result1 = pointFacade.savePoint(request);

        // Then: 첫 번째 결과 확인
        assertThat(result1.balance()).isEqualTo(7000L);
        verify(savePointUseCase, times(1)).savePoint(request);

        // Given: 두 번째 호출 - 이제 캐시에 있음
        reset(cacheRepository, getIdempotencyKey, distributedLock, savePointUseCase);
        when(cacheRepository.get(eq("point:result:" + requestId), eq(PointSaveResponse.class)))
                .thenReturn(Optional.of(firstResponse)); // 캐시 히트

        // When: 두 번째 호출
        PointSaveResponse result2 = pointFacade.savePoint(request);

        // Then: 같은 결과 반환, UseCase는 호출되지 않음
        assertThat(result2).isEqualTo(result1);
        assertThat(result2.balance()).isEqualTo(7000L);
        verify(savePointUseCase, never()).savePoint(any());
        verify(distributedLock, never()).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PointSaveResponse>>any());
    }

    @Test
    @DisplayName("분산 락을 userId 기반으로 획득한다")
    void 분산락_userId_기반_획득() {
        // Given
        String requestId = UUID.randomUUID().toString();
        String userId = "user-123";
        Long historyId = 600L;
        PointRequest request = new PointRequest(requestId, userId, 1000L, UseType.CHARGE);
        PointSaveResponse expectedResponse = new PointSaveResponse(historyId, userId, 11000L);

        when(cacheRepository.get(anyString(), eq(PointSaveResponse.class))).thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, userId, ResourceType.POINT)).thenReturn(Optional.empty());
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PointSaveResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PointSaveResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });
        when(savePointUseCase.savePoint(request)).thenReturn(expectedResponse);

        // When
        pointFacade.savePoint(request);

        // Then: userId 기반 락 키 사용 확인
        verify(distributedLock).executeWithLock(
                eq("point:user:" + userId),
                eq(3L),
                eq(10L),
                eq(TimeUnit.SECONDS),
                ArgumentMatchers.<Supplier<PointSaveResponse>>any()
        );
    }
}
