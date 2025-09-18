package com.gomdol.point.domain;

import com.gomdol.concert.point.domain.history.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class PointHistoryTest {

    private static final String FIXED_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String FIXED_REQUEST_ID = "550e8400-e29b-41d4-a716-446655440000";

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"asda", "   ", "", "asdsadasdasdasdasdasdasd-1241242132132asdasdasd" })
    void userId가_잘못된_데이터로_들어오면_에러를_발생_시킨다(String invalidUserId) {
        // when&then
        assertThatThrownBy(() -> PointHistory.create(invalidUserId, FIXED_REQUEST_ID,10000L,  UseType.CHARGE, 10000L, 10000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("요청한 사용자 ID가 올바른 형식이 아닙니다.");
    }

    @Test
    void 사용_이력_생성시_음수_금액으로_기록된다() {
        // given
        Long userId = 1L;
        long amount = 1000L;
        long beforeBalance = 10000L;
        long afterBalance = 9000L;

        // when
        PointHistory history = PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, amount, UseType.USE, beforeBalance, afterBalance);

        // then
        assertThat(history.getUserId()).isEqualTo(FIXED_UUID);
        assertThat(history.getAmount()).isEqualTo(-1000L);
        assertThat(history.getUseType()).isEqualTo(UseType.USE);
    }

    @Test
    void 충전_이력_생성시_양수_금액으로_기록된다() {
        // given
        long amount = 1000L;
        long beforeBalance = 8000L;
        long afterBalance = 9000L;

        // when
        PointHistory history = PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, amount, UseType.CHARGE, beforeBalance, afterBalance);

        // then
        assertThat(history.getUserId()).isEqualTo(FIXED_UUID);
        assertThat(history.getAmount()).isEqualTo(1000L);
        assertThat(history.getUseType()).isEqualTo(UseType.CHARGE);
    }

    @Test
    void 환불_이력_생성시_양수_금액으로_기록된다() {
        // given
        long amount = 1000L;
        long beforeBalance = 8000L;
        long afterBalance = 9000L;

        // when
        PointHistory history = PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, amount, UseType.REFUND, beforeBalance, afterBalance);

        // then
        assertThat(history.getUserId()).isEqualTo(FIXED_UUID);
        assertThat(history.getAmount()).isEqualTo(1000L);
        assertThat(history.getUseType()).isEqualTo(UseType.REFUND);
    }

    @Test
    void 음수_금액으로_이력을_생성하면_에러를_발생시킨다() {
        // given
        Long id = 1L;
        long negativeAmount = -1000L;

        // when & then
        assertThatThrownBy(() -> PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, negativeAmount, UseType.USE, 0L, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 양수여야 합니다.");

        assertThatThrownBy(() -> PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, negativeAmount, UseType.CHARGE, 0L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 양수여야 합니다.");

        assertThatThrownBy(() -> PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID, negativeAmount, UseType.REFUND, 0L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 양수여야 합니다.");
    }

    @Test
    void 잔액이_음수이면_에러를_발생시킨다() {
        // given
        Long id = 1L;
        long negativeBalance = -1000L;

        // when & then
        assertThatThrownBy(() -> PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID,1000L, UseType.USE, 10000L, negativeBalance))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액은 양수여야 합니다.");
    }

    @Test
    void 이전잔액이_음수이면_에러를_발생시킨다() {
        // given
        Long id = 1L;
        long negativeBalance = -1000L;

        // when & then
        assertThatThrownBy(() -> PointHistory.create(FIXED_UUID, FIXED_REQUEST_ID,1000L, UseType.USE, negativeBalance, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액은 양수여야 합니다.");
    }

}
