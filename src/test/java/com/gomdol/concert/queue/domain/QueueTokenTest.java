package com.gomdol.concert.queue.domain;

import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QueueToken 도메인 테스트")
class QueueTokenTest {

    @Test
    void 새로운_QueueToken을_생성한다() {
        // given
        String token = "abc123xyz789";
        String userId = "user123";
        Long targetId = 1L;
        QueueStatus status = QueueStatus.WAITING;
        long position = 42L;
        long ttlSeconds = 1800L;

        // when
        QueueToken queueToken = QueueToken.create(token, userId, targetId, status, position, ttlSeconds);

        // then
        assertThat(queueToken).isNotNull();
        assertThat(queueToken.getId()).isEqualTo(0L);  // create는 id=0
        assertThat(queueToken.getToken()).isEqualTo(token);
        assertThat(queueToken.getUserId()).isEqualTo(userId);
        assertThat(queueToken.getTargetId()).isEqualTo(targetId);
        assertThat(queueToken.getStatus()).isEqualTo(status);
        assertThat(queueToken.getPosition()).isEqualTo(position);
        assertThat(queueToken.getTtlSeconds()).isEqualTo(ttlSeconds);
    }

    @Test
    void QueueToken을_재구성한다() {
        // given
        Long id = 101L;
        String token = "abc123xyz789";
        String userId = "user123";
        Long targetId = 1L;
        QueueStatus status = QueueStatus.ENTERED;
        long position = 0L;
        long ttlSeconds = 180L;

        // when
        QueueToken queueToken = QueueToken.of(id, token, userId, targetId, status, position, ttlSeconds);

        // then
        assertThat(queueToken).isNotNull();
        assertThat(queueToken.getId()).isEqualTo(id);  // of는 실제 id
        assertThat(queueToken.getToken()).isEqualTo(token);
        assertThat(queueToken.getUserId()).isEqualTo(userId);
        assertThat(queueToken.getTargetId()).isEqualTo(targetId);
        assertThat(queueToken.getStatus()).isEqualTo(status);
        assertThat(queueToken.getPosition()).isEqualTo(position);
        assertThat(queueToken.getTtlSeconds()).isEqualTo(ttlSeconds);
    }

    @Test
    void EXPIRED_상태이면_isExpired가_true를_반환한다() {
        // given
        QueueToken expiredToken = QueueToken.create("token", "user", 1L, QueueStatus.EXPIRED, 0L, 0L);

        // when & then
        assertThat(expiredToken.isExpired()).isTrue();
    }

    @Test
    void TTL이_0이하이면_isExpired가_true를_반환한다() {
        // given
        QueueToken tokenWithZeroTtl = QueueToken.create("token", "user", 1L, QueueStatus.WAITING, 10L, 0L);
        QueueToken tokenWithNegativeTtl = QueueToken.create("token", "user", 1L, QueueStatus.WAITING, 10L, -10L);

        // when & then
        assertThat(tokenWithZeroTtl.isExpired()).isTrue();
        assertThat(tokenWithNegativeTtl.isExpired()).isTrue();
    }

    @Test
    void WAITING_상태이고_TTL이_남아있으면_isExpired가_false를_반환한다() {
        // given
        QueueToken waitingToken = QueueToken.create("token", "user", 1L, QueueStatus.WAITING, 10L, 1800L);

        // when & then
        assertThat(waitingToken.isExpired()).isFalse();
    }

    @Test
    void ENTERED_상태이고_TTL이_남아있으면_isExpired가_false를_반환한다() {
        // given
        QueueToken enteredToken = QueueToken.create("token", "user", 1L, QueueStatus.ENTERED, 0L, 180L);

        // when & then
        assertThat(enteredToken.isExpired()).isFalse();
    }

    @Test
    void entered_호출_시_상태가_ENTERED로_변경되고_TTL이_업데이트된다() {
        // given
        QueueToken waitingToken = QueueToken.create("token", "user", 1L, QueueStatus.WAITING, 10L, 1800L);
        long newTtl = 180L;

        // when
        waitingToken.entered(newTtl);

        // then
        assertThat(waitingToken.getStatus()).isEqualTo(QueueStatus.ENTERED);
        assertThat(waitingToken.getTtlSeconds()).isEqualTo(newTtl);
    }

    @Test
    void expired_호출_시_상태가_EXPIRED로_변경된다() {
        // given
        QueueToken token = QueueToken.create(
                "token", "user", 1L, QueueStatus.WAITING, 10L, 0L
        );

        // when
        token.expired();

        // then
        assertThat(token.getStatus()).isEqualTo(QueueStatus.EXPIRED);
    }

    @Test
    void ENTERED_상태에서도_expired_호출_가능() {
        // given
        QueueToken enteredToken = QueueToken.create("token", "user", 1L, QueueStatus.ENTERED, 0L, 1L);

        // when
        enteredToken.expired();

        // then
        assertThat(enteredToken.getStatus()).isEqualTo(QueueStatus.EXPIRED);
    }
}