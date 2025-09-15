package com.gomdol.point.domain;

import com.gomdol.concert.point.domain.history.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class PointHistoryTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1000299L, -1L, 0L})
    void userId가_잘못된_데이터로_들어오면_에러를_발생_시킨다(Long userId) {
        // when&then
        assertThatThrownBy(() -> PointHistory.create(1L, userId, 10000L,  UseType.CHARGE, 10000L, 10000L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID는 null이거나 0보다 작을 수 없습니다.");
    }

    @Test
    void 사용_이력_생성시_음수_금액으로_기록된다() {
        // given
        Long userId = 1L;
        long amount = 1000L;
        long beforeBalance = 10000L;
        long afterBalance = 9000L;

        // when
        PointHistory history = PointHistory.create(1L, userId, amount, UseType.USE, beforeBalance, afterBalance, LocalDateTime.now());

        // then
        assertThat(history.getUserId()).isEqualTo(1L);
        assertThat(history.getAmount()).isEqualTo(-1000L);
        assertThat(history.getUseType()).isEqualTo(UseType.USE);
    }

    @Test
    void 충전_이력_생성시_양수_금액으로_기록된다() {
        // given
        Long userId = 1L;
        long amount = 1000L;
        long beforeBalance = 8000L;
        long afterBalance = 9000L;

        // when
        PointHistory history = PointHistory.create(1L, userId, amount, UseType.CHARGE, beforeBalance, afterBalance, LocalDateTime.now());

        // then
        assertThat(history.getUserId()).isEqualTo(1L);
        assertThat(history.getAmount()).isEqualTo(1000L);
        assertThat(history.getUseType()).isEqualTo(UseType.CHARGE);
    }

    @Test
    void 환불_이력_생성시_양수_금액으로_기록된다() {
        // given
        Long userId = 1L;
        long amount = 1000L;
        long beforeBalance = 8000L;
        long afterBalance = 9000L;

        // when
        PointHistory history = PointHistory.create(1L, userId, amount, UseType.REFUND, beforeBalance, afterBalance, LocalDateTime.now());

        // then
        assertThat(history.getUserId()).isEqualTo(1L);
        assertThat(history.getAmount()).isEqualTo(1000L);
        assertThat(history.getUseType()).isEqualTo(UseType.REFUND);
    }

    @Test
    void 음수_금액으로_이력을_생성하면_에러를_발생시킨다() {
        // given
        Long userId = 1L;
        long negativeAmount = -1000L;

        // when & then
        assertThatThrownBy(() -> PointHistory.create(userId, userId, negativeAmount, UseType.USE, 0L, 1000L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 양수여야 합니다.");

        assertThatThrownBy(() -> PointHistory.create(userId, userId, negativeAmount, UseType.CHARGE, 0L, 0L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 양수여야 합니다.");

        assertThatThrownBy(() -> PointHistory.create(userId,  userId, negativeAmount, UseType.REFUND, 0L, 0L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 양수여야 합니다.");
    }

    @Test
    void 잔액이_음수이면_에러를_발생시킨다() {
        // given
        Long userId = 1L;
        long negativeBalance = -1000L;

        // when & then
        assertThatThrownBy(() -> PointHistory.create(userId, userId, 1000L, UseType.USE, 10000L, negativeBalance, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액은 양수여야 합니다.");
    }

    @Test
    void 이전잔액이_음수이면_에러를_발생시킨다() {
        // given
        Long userId = 1L;
        long negativeBalance = -1000L;

        // when & then
        assertThatThrownBy(() -> PointHistory.create(userId, userId, 1000L, UseType.USE, negativeBalance, 1000L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액은 양수여야 합니다.");
    }

}
