package com.gomdol.concert.queue.application;

import com.gomdol.concert.queue.application.port.in.IssueQueueTokenPort;
import com.gomdol.concert.queue.application.port.out.QueuePolicyProvider;
import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.application.port.out.TokenGenerator;
import com.gomdol.concert.queue.application.usecase.IssueQueueTokenUseCase;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 토큰 발급 UseCase 테스트")
class IssueQueueTokenUseCaseTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private QueuePolicyProvider queuePolicyProvider;

    @InjectMocks
    private IssueQueueTokenUseCase issueQueueTokenUseCase;

    @Test
    void 대기_중인_사람이_있으면_WAITING_상태로_토큰을_발급한다() {
        // given
        Long targetId = 1L;
        String userId = "user123";
        String generatedToken = "abc123xyz789";
        Long waitingTtl = 1800L;

        IssueQueueTokenPort.IssueCommand command = new IssueQueueTokenPort.IssueCommand(userId, targetId, "idempotency-key");

        // 기존 토큰 없음
        given(queueRepository.findByTargetIdAndUserId(targetId, userId)).willReturn(Optional.empty());

        // 대기 중인 사람 있음
        given(queueRepository.isWaiting(targetId)).willReturn(true);
        given(queuePolicyProvider.waitingTtlSeconds()).willReturn(waitingTtl);
        given(tokenGenerator.newToken()).willReturn(generatedToken);

        // 토큰 발급
        QueueToken issuedToken = QueueToken.create(generatedToken, userId, targetId, QueueStatus.WAITING, 42L, waitingTtl);
        given(queueRepository.issueToken(eq(targetId), eq(userId), eq(generatedToken),
                eq(QueueStatus.WAITING), eq(waitingTtl)))
                .willReturn(issuedToken);

        // when
        QueueTokenResponse response = issueQueueTokenUseCase.issue(command);

        // then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(QueueStatus.WAITING.name());
        assertThat(response.token()).isEqualTo(generatedToken);
        assertThat(response.position()).isEqualTo(42L);
        assertThat(response.ttlSeconds()).isEqualTo(waitingTtl);

        verify(queueRepository).findByTargetIdAndUserId(targetId, userId);
        verify(queueRepository).isWaiting(targetId);
        verify(queuePolicyProvider).waitingTtlSeconds();
        verify(queuePolicyProvider, never()).enteredTtlSeconds();
        verify(tokenGenerator).newToken();
        verify(queueRepository).issueToken(targetId, userId, generatedToken, QueueStatus.WAITING, waitingTtl);
    }

    @Test
    void 대기_중인_사람이_없으면_ENTERED_상태로_토큰을_발급한다() {
        // given
        Long targetId = 2L;
        String userId = "user456";
        String generatedToken = "xyz456abc123";
        Long enteredTtl = 180L;

        IssueQueueTokenPort.IssueCommand command = new IssueQueueTokenPort.IssueCommand(userId, targetId, "idempotency-key");

        // 기존 토큰 없음
        given(queueRepository.findByTargetIdAndUserId(targetId, userId)).willReturn(Optional.empty());

        // 대기 중인 사람 없음
        given(queueRepository.isWaiting(targetId)).willReturn(false);
        given(queuePolicyProvider.enteredTtlSeconds()).willReturn(enteredTtl);
        given(tokenGenerator.newToken()).willReturn(generatedToken);

        // 토큰 발급 (바로 입장)
        QueueToken issuedToken = QueueToken.create(generatedToken, userId, targetId, QueueStatus.ENTERED, 0L, enteredTtl);
        given(queueRepository.issueToken(eq(targetId), eq(userId), eq(generatedToken),
                eq(QueueStatus.ENTERED), eq(enteredTtl)))
                .willReturn(issuedToken);

        // when
        QueueTokenResponse response = issueQueueTokenUseCase.issue(command);

        // then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(QueueStatus.ENTERED.name());
        assertThat(response.token()).isEqualTo(generatedToken);
        assertThat(response.position()).isEqualTo(0L);
        assertThat(response.ttlSeconds()).isEqualTo(enteredTtl);

        verify(queueRepository).findByTargetIdAndUserId(targetId, userId);
        verify(queueRepository).isWaiting(targetId);
        verify(queuePolicyProvider).enteredTtlSeconds();
        verify(queuePolicyProvider, never()).waitingTtlSeconds();
        verify(tokenGenerator).newToken();
        verify(queueRepository).issueToken(targetId, userId, generatedToken, QueueStatus.ENTERED, enteredTtl);
    }

    @Test
    void 이미_발급된_토큰이_있으면_기존_토큰을_반환한다() {
        // given
        Long targetId = 1L;
        String userId = "user123";
        String existingToken = "existing-token";

        IssueQueueTokenPort.IssueCommand command = new IssueQueueTokenPort.IssueCommand(userId, targetId, "idempotency-key");

        // 기존 토큰 있음
        QueueToken existingQueueToken = QueueToken.create(existingToken, userId, targetId, QueueStatus.WAITING, 10L, 1500L);
        given(queueRepository.findByTargetIdAndUserId(targetId, userId)).willReturn(Optional.of(existingQueueToken));

        // when
        QueueTokenResponse response = issueQueueTokenUseCase.issue(command);

        // then
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo(existingToken);
        assertThat(response.status()).isEqualTo(QueueStatus.WAITING.name());
        assertThat(response.position()).isEqualTo(10L);
        assertThat(response.ttlSeconds()).isEqualTo(1500L);

        // 멱등성: 기존 토큰 반환 후 더 이상 처리하지 않음
        verify(queueRepository).findByTargetIdAndUserId(targetId, userId);
        verify(queueRepository, never()).isWaiting(anyLong());
        verify(queueRepository, never()).issueToken(anyLong(), anyString(), anyString(), any(), anyLong());
        verify(tokenGenerator, never()).newToken();
    }
}
