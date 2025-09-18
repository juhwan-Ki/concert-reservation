package com.gomdol.point.domain;

import com.gomdol.concert.point.domain.point.Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class PointTest {

    private static final String FIXED_UUID = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    void 유효한_값으로_포인트를_생성한다() {
        // given
        Long balance = 10000L;

        // when
        Point point = Point.create(FIXED_UUID, balance);

        // then
        assertThat(point.getUserId()).isEqualTo(FIXED_UUID);
        assertThat(point.getBalance()).isEqualTo(10000L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"asda", "   ", "", "asdsadasdasdasdasdasdasd-1241242132132asdasdasd" })
    void 존재하지_않는_사용자로_포인트_생성시_에러를_발생시킨다(String invalidUserId) {
        // when & then
        assertThatThrownBy(() -> Point.create(invalidUserId, 10000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("요청한 사용자 ID가 올바른 형식이 아닙니다.");
    }

    @Test
    public void 음수_포인트로_생성시_에러를_발생시킨다() throws Exception {
        // when & then
        assertThatThrownBy(() -> Point.create(FIXED_UUID, -1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("초기 잔액은 음수일 수 없습니다");
    }

    @Test
    public void 사용자가_자신의_포인트를_조회한다() throws Exception {
        // given
        Point point = Point.create(FIXED_UUID, 10000L);
        // when & then
        assertThat(point.getBalance()).isEqualTo(10000);
        assertThat(point.getUserId()).isEqualTo(FIXED_UUID);
    }

    @Test
    public void 포인트가_0인_사용자가_포인트를_충전하면_잔액이_증가한다() throws Exception {
        // given
        Point point = Point.create(FIXED_UUID, 0L);
        // when
        point.changeBalance(10000);
        // then
        assertThat(point.getBalance()).isEqualTo(10000);
        assertThat(point.getUserId()).isEqualTo(FIXED_UUID);
    }

    @Test
    public void 보유_포인트가_있는_사용자가_포인트_충전을_하면_포인트가_잔액이_증가한다() throws Exception {
        // given
        Point point = Point.create(FIXED_UUID, 10000L);
        // when
        point.changeBalance(10000);
        // then
        assertThat(point.getBalance()).isEqualTo(20000);
        assertThat(point.getUserId()).isEqualTo(FIXED_UUID);
    }

    @Test
    public void 포인트_사용시_잔액이_차감된다() throws Exception {
        // given
        Point point = Point.create(FIXED_UUID, 10000L);
        // when
        point.usePoint(3000);
        // then
        assertThat(point.getBalance()).isEqualTo(7000);
        assertThat(point.getUserId()).isEqualTo(FIXED_UUID);
    }

    @Test
    public void 보유_포인트보다_사용금액이_많으면_에러가_발생한다() throws Exception {
        // given
        Point point = Point.create(FIXED_UUID, 3000L);
        // when & then
        assertThatThrownBy(() -> point.usePoint(10000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액이 부족합니다.");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1000L, -1L, 0L})
    void 충전_금액이_양수가_아닌_경우_예외를_발생한다(long chargeAmount) {
        // given
        Point point = Point.create(FIXED_UUID, 10000L);
        // when&then
        assertThatThrownBy(() -> point.changeBalance(chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전/사용금액은 0보다 커야합니다.");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1000L, -1L, 0L})
    void 사용_금액이_양수가_아닌_경우_예외를_발생한다(long useAmount) {
        // given
        Point point = Point.create(FIXED_UUID, 10000L);
        // when&then
        assertThatThrownBy(() -> point.usePoint(useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전/사용금액은 0보다 커야합니다.");
    }

    // 최대 충전 금액은 100만원으로 한다
    @ParameterizedTest
    @ValueSource(longs = {1000001L, 99999999L})
    void 충전_금액이_최대_충전_금액보다_많은_경우_예외를_발생한다(long chargeAmount) {
        // given
        Point point = Point.create(FIXED_UUID, 10000L);

        // when&then
        assertThatThrownBy(() -> point.changeBalance(chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 충전 금액을 초과하였습니다.");
    }

    @ParameterizedTest
    @ValueSource(longs = {100L, 9999L, 10001L, 19999L, 20001L})
    void 충전_단위에_맞지_않으면_포인트_충전_시_예외를_발생한다(long chargeAmount) {
        // given
        Point point = Point.create(FIXED_UUID, chargeAmount);
        // when&then
        assertThatThrownBy(() -> point.usePoint(chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최소 사용 단위보다 작은 단위입니다.");
    }
}
