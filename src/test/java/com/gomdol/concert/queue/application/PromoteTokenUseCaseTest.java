package com.gomdol.concert.queue.application;

import com.gomdol.concert.queue.application.port.out.QueuePolicyProvider;
import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.application.usecase.PromoteTokenUseCase;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 토큰 승급 UseCase 테스트")
class PromoteTokenUseCaseTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private QueuePolicyProvider queuePolicyProvider;

    @InjectMocks
    private PromoteTokenUseCase promoteTokenUseCase;

    @Test
    void 남은_자리가_있으면_WAITING_토큰을_ENTERED로_승급한다() {
        // given
        Long targetId = 1L;
        int capacity = 50;
        long currentEntered = 30;
        int expectedPromoteCount = 20;
        long enteredTtl = 180L;

        // 현재 ENTERED 수
        given(queuePolicyProvider.capacity()).willReturn(capacity);
        given(queuePolicyProvider.enteredTtlSeconds()).willReturn(enteredTtl);
        given(queueRepository.countEnteredActiveWithLock(eq(targetId), any(Instant.class)))
                .willReturn(currentEntered);

        // WAITING 토큰 목록 (20명)
        List<QueueToken> waitingTokens = List.of(
                QueueToken.of(1L, "token1", "user1", targetId, QueueStatus.WAITING, 1L, 1800L),
                QueueToken.of(2L, "token2", "user2", targetId, QueueStatus.WAITING, 2L, 1800L),
                QueueToken.of(3L, "token3", "user3", targetId, QueueStatus.WAITING, 3L, 1800L)
        );
        given(queueRepository.findAndLockWaitingTokens(
                eq(targetId), any(Instant.class), eq(expectedPromoteCount)))
                .willReturn(waitingTokens);

        // when
        int promoted = promoteTokenUseCase.promote(targetId);

        // then
        verify(queueRepository, times(waitingTokens.size())).save(any(QueueToken.class));
        verify(queueRepository).countEnteredActiveWithLock(eq(targetId), any(Instant.class));
        verify(queueRepository).findAndLockWaitingTokens(eq(targetId), any(Instant.class), eq(expectedPromoteCount));
    }

    @Test
    void 남은_자리가_없으면_승급하지_않는다() {
        // given
        Long targetId = 1L;
        int capacity = 50;
        long currentEntered = 50;  // 꽉 참

        given(queuePolicyProvider.capacity()).willReturn(capacity);
        given(queueRepository.countEnteredActiveWithLock(eq(targetId), any(Instant.class))).willReturn(currentEntered);

        // when
        int promoted = promoteTokenUseCase.promote(targetId);

        // then
        assertThat(promoted).isEqualTo(0);

        // 승급 시도하지 않음
        verify(queueRepository).countEnteredActiveWithLock(eq(targetId), any(Instant.class));
        verify(queueRepository, never()).findAndLockWaitingTokens(anyLong(), any(Instant.class), anyInt());
        verify(queueRepository, never()).save(any(QueueToken.class));
    }

    @Test
    void capacity보다_현재_입장자가_많으면_승급하지_않는다() {
        // given
        Long targetId = 1L;
        int capacity = 50;
        long currentEntered = 60;  // 초과

        given(queuePolicyProvider.capacity()).willReturn(capacity);
        given(queueRepository.countEnteredActiveWithLock(eq(targetId), any(Instant.class))).willReturn(currentEntered);

        // when
        int promoted = promoteTokenUseCase.promote(targetId);

        // then
        assertThat(promoted).isEqualTo(0);

        verify(queueRepository).countEnteredActiveWithLock(eq(targetId), any(Instant.class));
        verify(queueRepository, never()).findAndLockWaitingTokens(anyLong(), any(Instant.class), anyInt());
    }

    @Test
    void 대기_중인_토큰이_없으면_승급하지_않는다() {
        // given
        Long targetId = 1L;
        int capacity = 50;
        long currentEntered = 30;
        int expectedPromoteCount = 20;

        given(queuePolicyProvider.capacity()).willReturn(capacity);
        given(queueRepository.countEnteredActiveWithLock(eq(targetId), any(Instant.class))).willReturn(currentEntered);

        // WAITING 토큰 없음
        given(queueRepository.findAndLockWaitingTokens(
                eq(targetId), any(Instant.class), eq(expectedPromoteCount)))
                .willReturn(List.of());

        // when
        int promoted = promoteTokenUseCase.promote(targetId);

        // then
        assertThat(promoted).isEqualTo(0);

        verify(queueRepository).countEnteredActiveWithLock(eq(targetId), any(Instant.class));
        verify(queueRepository).findAndLockWaitingTokens(eq(targetId), any(Instant.class), eq(expectedPromoteCount));
        verify(queueRepository, never()).save(any(QueueToken.class));
    }

    @Test
    void 남은_자리보다_대기자가_적으면_대기자_수만큼만_승급한다() {
        // given
        Long targetId = 1L;
        int capacity = 50;
        long currentEntered = 30;
        int expectedPromoteCount = 20;
        long enteredTtl = 180L;

        given(queuePolicyProvider.capacity()).willReturn(capacity);
        given(queuePolicyProvider.enteredTtlSeconds()).willReturn(enteredTtl);
        given(queueRepository.countEnteredActiveWithLock(eq(targetId), any(Instant.class))).willReturn(currentEntered);

        // WAITING 토큰 5명만 있음 (남은 자리 20개보다 적음)
        List<QueueToken> waitingTokens = List.of(
                QueueToken.of(1L, "token1", "user1", targetId, QueueStatus.WAITING, 1L, 1800L),
                QueueToken.of(2L, "token2", "user2", targetId, QueueStatus.WAITING, 2L, 1800L),
                QueueToken.of(3L, "token3", "user3", targetId, QueueStatus.WAITING, 3L, 1800L),
                QueueToken.of(4L, "token4", "user4", targetId, QueueStatus.WAITING, 4L, 1800L),
                QueueToken.of(5L, "token5", "user5", targetId, QueueStatus.WAITING, 5L, 1800L)
        );
        given(queueRepository.findAndLockWaitingTokens(
                eq(targetId), any(Instant.class), eq(expectedPromoteCount)))
                .willReturn(waitingTokens);

        // when
        int promoted = promoteTokenUseCase.promote(targetId);

        // then
        verify(queueRepository, times(5)).save(any(QueueToken.class));
    }
}