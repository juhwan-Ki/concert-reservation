package com.gomdol.concert.payment.application.facade;

import com.gomdol.concert.common.application.cache.port.out.CacheRepository;
import com.gomdol.concert.common.application.idempotency.port.in.GetIdempotencyKey;
import com.gomdol.concert.common.application.lock.port.out.DistributedLock;
import com.gomdol.concert.common.domain.idempotency.ResourceType;
import com.gomdol.concert.payment.application.port.in.SavePaymentPort.PaymentCommand;
import com.gomdol.concert.payment.application.usecase.PaymentQueryUseCase;
import com.gomdol.concert.payment.application.usecase.SavePaymentUseCase;
import com.gomdol.concert.payment.domain.PaymentStatus;
import com.gomdol.concert.payment.domain.model.Payment;
import com.gomdol.concert.payment.presentation.dto.PaymentResponse;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentFacade 테스트")
class PaymentFacadeTest {

    @Mock
    private CacheRepository cacheRepository;

    @Mock
    private GetIdempotencyKey getIdempotencyKey;

    @Mock
    private DistributedLock distributedLock;

    @Mock
    private SavePaymentUseCase savePaymentUseCase;

    @Mock
    private PaymentQueryUseCase paymentQueryUseCase;

    @InjectMocks
    private PaymentFacade paymentFacade;

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Long RESERVATION_ID = 100L;
    private static final Long AMOUNT = 50000L;

    @Test
    @DisplayName("Redis 캐시에 결과가 있으면 즉시 반환한다")
    void redis_캐시_히트_시_즉시_반환() {
        // Given
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(RESERVATION_ID, USER_ID, requestId, AMOUNT);
        PaymentResponse cachedResponse = new PaymentResponse(1L, RESERVATION_ID, "SUCCEEDED", AMOUNT, LocalDateTime.now());

        when(cacheRepository.get(eq("payment:result:" + requestId), eq(PaymentResponse.class)))
                .thenReturn(Optional.of(cachedResponse));

        // When
        PaymentResponse result = paymentFacade.processPayment(command);

        // Then
        assertThat(result).isEqualTo(cachedResponse);
        assertThat(result.amount()).isEqualTo(AMOUNT);

        // 캐시 히트 시 DB 조회나 락 획득 없이 바로 반환
        verify(cacheRepository).get(anyString(), eq(PaymentResponse.class));
        verify(getIdempotencyKey, never()).getIdempotencyKey(anyString(), anyString(), any());
        verify(distributedLock, never()).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PaymentResponse>>any());
    }

    @Test
    @DisplayName("DB 멱등키가 존재하면 기존 결제를 조회하여 반환한다")
    void DB_멱등키_존재_시_기존_결제_반환() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long paymentId = 200L;
        PaymentCommand command = new PaymentCommand(RESERVATION_ID, USER_ID, requestId, AMOUNT);

        when(cacheRepository.get(anyString(), eq(PaymentResponse.class)))
                .thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT))
                .thenReturn(Optional.of(paymentId));

        Payment payment = Payment.of(paymentId, RESERVATION_ID, USER_ID, "PAY-CODE-123", requestId,
                AMOUNT, PaymentStatus.SUCCEEDED, LocalDateTime.now());
        when(paymentQueryUseCase.findById(paymentId))
                .thenReturn(Optional.of(payment));

        // When
        PaymentResponse result = paymentFacade.processPayment(command);

        // Then
        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(result.amount()).isEqualTo(AMOUNT);

        // 멱등키가 있으면 락 획득 없이 바로 반환
        verify(getIdempotencyKey).getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT);
        verify(paymentQueryUseCase).findById(paymentId);
        verify(cacheRepository).set(anyString(), any(PaymentResponse.class), any(Duration.class));
        verify(distributedLock, never()).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PaymentResponse>>any());
    }

    @Test
    @DisplayName("정상 처리: 결제 성공 후 캐시 저장")
    void 정상_처리_결제_성공() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long paymentId = 300L;
        PaymentCommand command = new PaymentCommand(RESERVATION_ID, USER_ID, requestId, AMOUNT);
        PaymentResponse expectedResponse = new PaymentResponse(paymentId, RESERVATION_ID, "SUCCEEDED", AMOUNT, LocalDateTime.now());

        when(cacheRepository.get(anyString(), eq(PaymentResponse.class)))
                .thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT))
                .thenReturn(Optional.empty());

        // DistributedLock mock: 콜백을 즉시 실행
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PaymentResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PaymentResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        when(savePaymentUseCase.processPayment(command)).thenReturn(expectedResponse);

        // When
        PaymentResponse result = paymentFacade.processPayment(command);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.amount()).isEqualTo(AMOUNT);
        assertThat(result.paymentId()).isEqualTo(paymentId);

        // 정상 처리 후 캐시 저장 확인
        verify(savePaymentUseCase).processPayment(command);
        verify(cacheRepository).set(eq("payment:result:" + requestId), eq(expectedResponse), any(Duration.class));
    }

    @Test
    @DisplayName("DB 제약조건 위반 시 멱등키로 재조회하여 반환")
    void DB_제약조건_위반_시_멱등키_재조회() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long paymentId = 400L;
        PaymentCommand command = new PaymentCommand(RESERVATION_ID, USER_ID, requestId, AMOUNT);

        when(cacheRepository.get(anyString(), eq(PaymentResponse.class)))
                .thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT))
                .thenReturn(Optional.empty()) // 첫 조회는 없음
                .thenReturn(Optional.of(paymentId)); // 재조회 시 존재

        // DistributedLock mock: 콜백을 즉시 실행
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PaymentResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PaymentResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        // SavePaymentUseCase가 DataIntegrityViolationException 발생
        when(savePaymentUseCase.processPayment(command))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        Payment payment = Payment.of(paymentId, RESERVATION_ID, USER_ID, "PAY-CODE-456", requestId,
                AMOUNT, PaymentStatus.SUCCEEDED, LocalDateTime.now());
        when(paymentQueryUseCase.findById(paymentId))
                .thenReturn(Optional.of(payment));

        // When
        PaymentResponse result = paymentFacade.processPayment(command);

        // Then
        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.amount()).isEqualTo(AMOUNT);

        // 제약조건 위반 시 멱등키로 재조회
        verify(savePaymentUseCase).processPayment(command);
        verify(getIdempotencyKey, times(2)).getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT);
        verify(paymentQueryUseCase).findById(paymentId);
        verify(cacheRepository).set(anyString(), any(PaymentResponse.class), any(Duration.class));
    }

    @Test
    @DisplayName("DB 제약조건 위반이지만 멱등키가 없으면 예외 발생")
    void DB_제약조건_위반_but_멱등키_없으면_예외() {
        // Given
        String requestId = UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(RESERVATION_ID, USER_ID, requestId, AMOUNT);

        when(cacheRepository.get(anyString(), eq(PaymentResponse.class)))
                .thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT))
                .thenReturn(Optional.empty()); // 처음에도 없고

        // DistributedLock mock: 콜백을 즉시 실행
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PaymentResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PaymentResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });

        DataIntegrityViolationException originalException = new DataIntegrityViolationException("Other constraint");
        when(savePaymentUseCase.processPayment(command)).thenThrow(originalException);

        // 재조회해도 멱등키가 없음
        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentFacade.processPayment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("결제 처리 중 제약조건 위반")
                .hasCause(originalException);

        verify(savePaymentUseCase).processPayment(command);
        verify(getIdempotencyKey, times(2)).getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT);
    }

    @Test
    @DisplayName("같은 멱등키로 두 번 호출 시 중복 처리 없이 같은 응답 반환")
    void 멱등성_보장_중복_처리_방지() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long paymentId = 500L;
        PaymentCommand command = new PaymentCommand(RESERVATION_ID, USER_ID, requestId, AMOUNT);
        PaymentResponse firstResponse = new PaymentResponse(paymentId, RESERVATION_ID, "SUCCEEDED", AMOUNT, LocalDateTime.now());

        // 첫 번째 호출
        when(cacheRepository.get(eq("payment:result:" + requestId), eq(PaymentResponse.class)))
                .thenReturn(Optional.empty()); // 첫 호출엔 캐시 없음
        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT))
                .thenReturn(Optional.empty());
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PaymentResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PaymentResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });
        when(savePaymentUseCase.processPayment(command)).thenReturn(firstResponse);

        // When: 첫 번째 호출
        PaymentResponse result1 = paymentFacade.processPayment(command);

        // Then: 첫 번째 결과 확인
        assertThat(result1.amount()).isEqualTo(AMOUNT);
        verify(savePaymentUseCase, times(1)).processPayment(command);

        // Given: 두 번째 호출 - 이제 캐시에 있음
        reset(cacheRepository, getIdempotencyKey, distributedLock, savePaymentUseCase);
        when(cacheRepository.get(eq("payment:result:" + requestId), eq(PaymentResponse.class)))
                .thenReturn(Optional.of(firstResponse)); // 캐시 히트

        // When: 두 번째 호출
        PaymentResponse result2 = paymentFacade.processPayment(command);

        // Then: 같은 결과 반환, UseCase는 호출되지 않음
        assertThat(result2).isEqualTo(result1);
        assertThat(result2.amount()).isEqualTo(AMOUNT);
        verify(savePaymentUseCase, never()).processPayment(any());
        verify(distributedLock, never()).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PaymentResponse>>any());
    }

    @Test
    @DisplayName("분산 락을 reservationId 기반으로 획득한다")
    void 분산락_reservationId_기반_획득() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long reservationId = 999L;
        Long paymentId = 600L;
        PaymentCommand command = new PaymentCommand(reservationId, USER_ID, requestId, AMOUNT);
        PaymentResponse expectedResponse = new PaymentResponse(paymentId, reservationId, "SUCCEEDED", AMOUNT, LocalDateTime.now());

        when(cacheRepository.get(anyString(), eq(PaymentResponse.class)))
                .thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT))
                .thenReturn(Optional.empty());
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), ArgumentMatchers.<Supplier<PaymentResponse>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<PaymentResponse> supplier = invocation.getArgument(4);
                    return supplier.get();
                });
        when(savePaymentUseCase.processPayment(command)).thenReturn(expectedResponse);

        // When
        paymentFacade.processPayment(command);

        // Then: reservationId 기반 락 키 사용 확인
        verify(distributedLock).executeWithLock(
                eq("payment:reservation:" + reservationId),
                eq(3L),
                eq(10L),
                eq(TimeUnit.SECONDS),
                ArgumentMatchers.<Supplier<PaymentResponse>>any()
        );
    }

    @Test
    @DisplayName("멱등키가 존재하지만 결제를 찾을 수 없으면 예외 발생")
    void 멱등키_존재하지만_결제_찾을수없으면_예외() {
        // Given
        String requestId = UUID.randomUUID().toString();
        Long paymentId = 700L;
        PaymentCommand command = new PaymentCommand(RESERVATION_ID, USER_ID, requestId, AMOUNT);

        when(cacheRepository.get(anyString(), eq(PaymentResponse.class)))
                .thenReturn(Optional.empty());
        when(getIdempotencyKey.getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT))
                .thenReturn(Optional.of(paymentId));
        when(paymentQueryUseCase.findById(paymentId))
                .thenReturn(Optional.empty()); // 결제를 찾을 수 없음

        // When & Then
        assertThatThrownBy(() -> paymentFacade.processPayment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("멱등성 키는 존재하나 결제를 찾을 수 없습니다");

        verify(getIdempotencyKey).getIdempotencyKey(requestId, USER_ID, ResourceType.PAYMENT);
        verify(paymentQueryUseCase).findById(paymentId);
    }
}
