package com.gomdol.concert.queue.application;

import com.gomdol.concert.queue.application.port.in.EnterQueuePort;
import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.application.usecase.EnterQueueUseCase;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;
import com.gomdol.concert.show.application.port.out.ShowQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 진입/상태 확인 UseCase 테스트")
class EnterQueueUseCaseTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private ShowQueryRepository showQueryRepository;

    @InjectMocks
    private EnterQueueUseCase enterQueueUseCase;

    @Test
    void WAITING_상태_토큰의_대기_정보를_조회한다() {
        // given
        Long targetId = 1L;
        String userId = "user123";
        String token = "abc123xyz789";

        EnterQueuePort.QueueTokenRequest request = new EnterQueuePort.QueueTokenRequest(targetId, userId, token);

        // 공연 존재
        given(showQueryRepository.existsById(targetId)).willReturn(true);

        // WAITING 상태 토큰
        QueueToken queueToken = QueueToken.create(token, userId, targetId, QueueStatus.WAITING, 42L, 1500L);
        given(queueRepository.findByTargetIdAndToken(targetId, token)).willReturn(Optional.of(queueToken));

        // when
        QueueTokenResponse response = enterQueueUseCase.enterQueue(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(QueueStatus.WAITING.name());
        assertThat(response.position()).isEqualTo(42L);
        assertThat(response.ttlSeconds()).isEqualTo(1500L);

        verify(showQueryRepository).existsById(targetId);
        verify(queueRepository).findByTargetIdAndToken(targetId, token);
    }

    @Test
    void ENTERED_상태_토큰의_입장_정보를_조회한다() {
        // given
        Long targetId = 1L;
        String userId = "user456";
        String token = "xyz456abc123";

        EnterQueuePort.QueueTokenRequest request = new EnterQueuePort.QueueTokenRequest(targetId, userId, token);

        // 공연 존재
        given(showQueryRepository.existsById(targetId)).willReturn(true);

        // ENTERED 상태 토큰
        QueueToken queueToken = QueueToken.create(token, userId, targetId, QueueStatus.ENTERED, 0L, 165L);
        given(queueRepository.findByTargetIdAndToken(targetId, token)).willReturn(Optional.of(queueToken));

        // when
        QueueTokenResponse response = enterQueueUseCase.enterQueue(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(QueueStatus.ENTERED.name());
        assertThat(response.position()).isEqualTo(0L);
        assertThat(response.ttlSeconds()).isEqualTo(165L);

        verify(showQueryRepository).existsById(targetId);
        verify(queueRepository).findByTargetIdAndToken(targetId, token);
    }

    @Test
    void 존재하지_않는_공연이면_예외를_발생시킨다() {
        // given
        Long targetId = 999L;
        String userId = "user123";
        String token = "abc123xyz789";

        EnterQueuePort.QueueTokenRequest request = new EnterQueuePort.QueueTokenRequest(targetId, userId, token);

        // 공연 없음
        given(showQueryRepository.existsById(targetId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> enterQueueUseCase.enterQueue(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 공연입니다");

        verify(showQueryRepository).existsById(targetId);
    }

    @Test
    void 토큰이_존재하지_않으면_예외를_발생시킨다() {
        // given
        Long targetId = 1L;
        String userId = "user123";
        String token = "invalid-token";

        EnterQueuePort.QueueTokenRequest request = new EnterQueuePort.QueueTokenRequest(targetId, userId, token);

        // 공연 존재
        given(showQueryRepository.existsById(targetId)).willReturn(true);

        // 토큰 없음
        given(queueRepository.findByTargetIdAndToken(targetId, token)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> enterQueueUseCase.enterQueue(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("대기열 토큰이 존재하지 않습니다");

        verify(showQueryRepository).existsById(targetId);
        verify(queueRepository).findByTargetIdAndToken(targetId, token);
    }

    @Test
    void 만료된_토큰이면_예외를_발생시킨다() {
        // given
        Long targetId = 1L;
        String userId = "user123";
        String token = "expired-token";

        EnterQueuePort.QueueTokenRequest request = new EnterQueuePort.QueueTokenRequest(targetId, userId, token);

        // 공연 존재
        given(showQueryRepository.existsById(targetId)).willReturn(true);

        // 만료된 토큰 (TTL = 0)
        QueueToken expiredToken = QueueToken.create(token, userId, targetId, QueueStatus.EXPIRED, 0L, 0L);
        given(queueRepository.findByTargetIdAndToken(targetId, token)).willReturn(Optional.of(expiredToken));

        // when & then
        assertThatThrownBy(() -> enterQueueUseCase.enterQueue(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("만료된 토큰입니다");

        verify(showQueryRepository).existsById(targetId);
        verify(queueRepository).findByTargetIdAndToken(targetId, token);
    }

    @Test
    void 토큰_소유자가_일치하지_않으면_예외를_발생시킨다() {
        // given
        Long targetId = 1L;
        String userId = "user123";
        String actualUserId = "user456";  // 실제 토큰 소유자
        String token = "abc123xyz789";

        EnterQueuePort.QueueTokenRequest request = new EnterQueuePort.QueueTokenRequest(targetId, userId, token);

        // 공연 존재
        given(showQueryRepository.existsById(targetId)).willReturn(true);

        // 다른 사용자의 토큰
        QueueToken queueToken = QueueToken.create(token, actualUserId, targetId, QueueStatus.WAITING, 10L, 1500L);
        given(queueRepository.findByTargetIdAndToken(targetId, token)).willReturn(Optional.of(queueToken));

        // when & then
        assertThatThrownBy(() -> enterQueueUseCase.enterQueue(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("토큰 소유자가 일치하지 않습니다");

        verify(showQueryRepository).existsById(targetId);
        verify(queueRepository).findByTargetIdAndToken(targetId, token);
    }
}