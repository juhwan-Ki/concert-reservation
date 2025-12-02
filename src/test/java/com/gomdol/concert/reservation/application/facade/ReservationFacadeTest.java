package com.gomdol.concert.reservation.application.facade;

import com.gomdol.concert.common.application.cache.port.out.CacheRepository;
import com.gomdol.concert.common.application.idempotency.port.in.GetIdempotencyKey;
import com.gomdol.concert.common.application.lock.port.out.DistributedLock;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort.ReservationSeatCommand;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.application.usecase.ReservationSeatUseCase;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationFacade 테스트")
class ReservationFacadeTest {

    @Mock
    private CacheRepository cacheRepository;

    @Mock
    private GetIdempotencyKey getIdempotencyKey;

    @Mock
    private DistributedLock distributedLock;

    @Mock
    private ReservationSeatUseCase reservationSeatUseCase;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationFacade reservationFacade;

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Long SHOW_ID = 100L;
    private static final List<Long> SEAT_IDS = List.of(1L, 2L);

    @Test
    @DisplayName("Redis 캐시에 결과가 있으면 즉시 반환한다")
    void redis_캐시_히트_시_즉시_반환() {
        // Given
        String requestId = UUID.randomUUID().toString();
        ReservationSeatCommand command = new ReservationSeatCommand(USER_ID, requestId, SHOW_ID, SEAT_IDS);
        ReservationResponse cachedResponse = new ReservationResponse(1L, "RES-001", requestId, LocalDateTime.now().plusMinutes(10));

        when(cacheRepository.get(eq("reservation:result:" + requestId), eq(ReservationResponse.class)))
                .thenReturn(Optional.of(cachedResponse));

        // When
        ReservationResponse result = reservationFacade.reservationSeat(command);

        // Then
        assertThat(result).isEqualTo(cachedResponse);
        assertThat(result.reservationId()).isEqualTo(1L);

        // 캐시 히트 시 DB 조회나 락 획득 없이 바로 반환
        verify(cacheRepository).get(anyString(), eq(ReservationResponse.class));
        verify(getIdempotencyKey, never()).getIdempotencyKey(anyString(), anyString(), any());
        verify(distributedLock, never()).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any());
    }

    @Test
    @DisplayName("DB 멱등키가 존재하면 기존 예약을 조회하여 반환한다")
    void DB_멱등키_존재_시_기존_예약_반환() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long reservationId = 200L;
        ReservationSeatCommand command = new ReservationSeatCommand(USER_ID, requestId, SHOW_ID, SEAT_IDS);

        when(cacheRepository.get(anyString(), eq(ReservationResponse.class)))
                .thenReturn(Optional.empty());

        // requestId 락 Mock
        when(distributedLock.executeWithLock(eq("reservation:request:" + requestId), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION))
                .thenReturn(Optional.of(reservationId));

        List<ReservationSeat> seats = List.of(
                ReservationSeat.create(reservationId, 1L, SHOW_ID),
                ReservationSeat.create(reservationId, 2L, SHOW_ID)
        );
        Reservation reservation = Reservation.of(reservationId, USER_ID, "RES-002", requestId, seats, 20000L, LocalDateTime.now().plusMinutes(10), null);

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        // When
        ReservationResponse result = reservationFacade.reservationSeat(command);

        // Then
        assertThat(result.reservationId()).isEqualTo(reservationId);
        assertThat(result.reservationCode()).isEqualTo("RES-002");

        // 멱등키가 있으면 좌석 락 획득 없이 바로 반환
        verify(getIdempotencyKey).getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION);
        verify(reservationRepository).findById(reservationId);
        verify(cacheRepository).set(anyString(), any(ReservationResponse.class), any(Duration.class));
        verify(distributedLock, times(1)).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any());
    }

    @Test
    @DisplayName("정상 처리: 예약 성공 후 캐시 저장")
    void 정상_처리_예약_성공() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long reservationId = 300L;
        ReservationSeatCommand command = new ReservationSeatCommand(USER_ID, requestId, SHOW_ID, SEAT_IDS);
        ReservationResponse expectedResponse = new ReservationResponse(reservationId, "RES-003", requestId,LocalDateTime.now().plusMinutes(10));

        when(cacheRepository.get(anyString(), eq(ReservationResponse.class)))
                .thenReturn(Optional.empty());

        // requestId 락 Mock
        when(distributedLock.executeWithLock(eq("reservation:request:" + requestId), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION))
                .thenReturn(Optional.empty());

        // 좌석 락 Mock
        when(distributedLock.executeWithLock(eq("reservation:show:100:seats:1,2"), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(reservationSeatUseCase.reservationSeat(command)).thenReturn(expectedResponse);

        // When
        ReservationResponse result = reservationFacade.reservationSeat(command);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.reservationId()).isEqualTo(reservationId);

        // 정상 처리 후 캐시 저장 확인
        verify(reservationSeatUseCase).reservationSeat(command);
        verify(cacheRepository).set(eq("reservation:result:" + requestId), eq(expectedResponse), any(Duration.class));
    }

    @Test
    @DisplayName("DB 제약조건 위반 시 멱등키로 재조회하여 반환")
    void DB_제약조건_위반_시_멱등키_재조회() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long reservationId = 400L;
        ReservationSeatCommand command = new ReservationSeatCommand(USER_ID, requestId, SHOW_ID, SEAT_IDS);

        when(cacheRepository.get(anyString(), eq(ReservationResponse.class)))
                .thenReturn(Optional.empty());

        // requestId 락 Mock
        when(distributedLock.executeWithLock(eq("reservation:request:" + requestId), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION))
                .thenReturn(Optional.empty()) // 첫 조회는 없음
                .thenReturn(Optional.of(reservationId)); // 재조회 시 존재

        // 좌석 락 Mock
        when(distributedLock.executeWithLock(eq("reservation:show:100:seats:1,2"), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        // UseCase가 DataIntegrityViolationException 발생
        when(reservationSeatUseCase.reservationSeat(command))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        List<ReservationSeat> seats = List.of(
                ReservationSeat.create(reservationId, 1L, SHOW_ID),
                ReservationSeat.create(reservationId, 2L, SHOW_ID)
        );
        Reservation reservation = Reservation.of(reservationId, USER_ID, "RES-004", requestId, seats, 20000L, LocalDateTime.now().plusMinutes(10), null);

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        // When
        ReservationResponse result = reservationFacade.reservationSeat(command);

        // Then
        assertThat(result.reservationId()).isEqualTo(reservationId);
        assertThat(result.reservationCode()).isEqualTo("RES-004");

        // 제약조건 위반 시 멱등키로 재조회
        verify(reservationSeatUseCase).reservationSeat(command);
        verify(getIdempotencyKey, times(2)).getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION);
        verify(reservationRepository).findById(reservationId);
        verify(cacheRepository).set(anyString(), any(ReservationResponse.class), any(Duration.class));
    }

    @Test
    @DisplayName("DB 제약조건 위반이지만 멱등키가 없으면 예외 발생")
    void DB_제약조건_위반_멱등키_없으면_예외() {
        // Given
        String requestId = UUID.randomUUID().toString();
        ReservationSeatCommand command = new ReservationSeatCommand(USER_ID, requestId, SHOW_ID, SEAT_IDS);

        when(cacheRepository.get(anyString(), eq(ReservationResponse.class)))
                .thenReturn(Optional.empty());

        // requestId 락 Mock
        when(distributedLock.executeWithLock(eq("reservation:request:" + requestId), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION)).thenReturn(Optional.empty()); // 처음에도 없고, 재조회해도 없음

        // 좌석 락 Mock
        when(distributedLock.executeWithLock(eq("reservation:show:100:seats:1,2"), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        DataIntegrityViolationException originalException = new DataIntegrityViolationException("Seat already reserved");
        when(reservationSeatUseCase.reservationSeat(command)).thenThrow(originalException);

        // When & Then
        assertThatThrownBy(() -> reservationFacade.reservationSeat(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 선택된 좌석입니다");

        verify(reservationSeatUseCase).reservationSeat(command);
        verify(getIdempotencyKey, times(2)).getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION);
    }

    @Test
    @DisplayName("같은 멱등키로 두 번 호출 시 중복 예약 없이 같은 응답 반환")
    void 멱등성_보장_중복_예약_방지() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long reservationId = 500L;
        ReservationSeatCommand command = new ReservationSeatCommand(USER_ID, requestId, SHOW_ID, SEAT_IDS);
        ReservationResponse firstResponse = new ReservationResponse(reservationId, "RES-005", requestId, LocalDateTime.now().plusMinutes(10));

        // 첫 번째 호출
        when(cacheRepository.get(eq("reservation:result:" + requestId), eq(ReservationResponse.class)))
                .thenReturn(Optional.empty()); // 첫 호출엔 캐시 없음

        when(distributedLock.executeWithLock(eq("reservation:request:" + requestId), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION))
                .thenReturn(Optional.empty());

        when(distributedLock.executeWithLock(eq("reservation:show:100:seats:1,2"), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(reservationSeatUseCase.reservationSeat(command)).thenReturn(firstResponse);

        // When: 첫 번째 호출
        ReservationResponse result1 = reservationFacade.reservationSeat(command);

        // Then: 첫 번째 결과 확인
        assertThat(result1.reservationId()).isEqualTo(reservationId);
        verify(reservationSeatUseCase, times(1)).reservationSeat(command);

        // Given: 두 번째 호출 - 이제 캐시에 있음
        reset(cacheRepository, getIdempotencyKey, distributedLock, reservationSeatUseCase);
        when(cacheRepository.get(eq("reservation:result:" + requestId), eq(ReservationResponse.class)))
                .thenReturn(Optional.of(firstResponse)); // 캐시 히트

        // When: 두 번째 호출
        ReservationResponse result2 = reservationFacade.reservationSeat(command);

        // Then: 같은 결과 반환, UseCase는 호출되지 않음
        assertThat(result2).isEqualTo(result1);
        assertThat(result2.reservationId()).isEqualTo(reservationId);
        verify(reservationSeatUseCase, never()).reservationSeat(any());
        verify(distributedLock, never()).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any());
    }

    @Test
    @DisplayName("분산 락을 requestId와 좌석 ID 기반으로 획득한다")
    void 분산락_requestId_좌석ID_기반_획득() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long reservationId = 600L;
        List<Long> seatIds = List.of(5L, 3L, 1L); // 정렬되지 않은 좌석 ID
        ReservationSeatCommand command = new ReservationSeatCommand(USER_ID, requestId, SHOW_ID, seatIds);
        ReservationResponse expectedResponse = new ReservationResponse(reservationId, "RES-006", requestId, LocalDateTime.now().plusMinutes(10));

        when(cacheRepository.get(anyString(), eq(ReservationResponse.class)))
                .thenReturn(Optional.empty());

        // requestId 락 Mock
        when(distributedLock.executeWithLock(eq("reservation:request:" + requestId), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION))
                .thenReturn(Optional.empty());

        // 좌석 락 Mock - 좌석 ID가 정렬되어 있어야 함
        when(distributedLock.executeWithLock(eq("reservation:show:100:seats:1,3,5"), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(reservationSeatUseCase.reservationSeat(command)).thenReturn(expectedResponse);

        // When
        reservationFacade.reservationSeat(command);

        // Then: requestId 락과 좌석 락 모두 확인
        verify(distributedLock).executeWithLock(
                eq("reservation:request:" + requestId),
                eq(3L),
                eq(10L),
                eq(TimeUnit.SECONDS),
                ArgumentMatchers.<Supplier<ReservationResponse>>any()
        );

        verify(distributedLock).executeWithLock(
                eq("reservation:show:100:seats:1,3,5"), // 정렬된 좌석 ID
                eq(3L),
                eq(10L),
                eq(TimeUnit.SECONDS),
                ArgumentMatchers.<Supplier<ReservationResponse>>any()
        );
    }

    @Test
    @DisplayName("멱등키가 존재하지만 예약을 찾을 수 없으면 예외 발생")
    void 멱등키_존재하지만_예약_찾을수없으면_예외() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long reservationId = 700L;
        ReservationSeatCommand command = new ReservationSeatCommand(USER_ID, requestId, SHOW_ID, SEAT_IDS);

        when(cacheRepository.get(anyString(), eq(ReservationResponse.class)))
                .thenReturn(Optional.empty());

        when(distributedLock.executeWithLock(eq("reservation:request:" + requestId), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<ReservationResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<ReservationResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION))
                .thenReturn(Optional.of(reservationId));

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.empty()); // 예약을 찾을 수 없음

        // When & Then
        assertThatThrownBy(() -> reservationFacade.reservationSeat(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("멱등성 키는 존재하나 예약을 찾을 수 없습니다");

        verify(getIdempotencyKey).getIdempotencyKey(requestId, USER_ID, ResourceType.RESERVATION);
        verify(reservationRepository).findById(reservationId);
    }
}
