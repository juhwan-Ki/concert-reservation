package com.gomdol.point.domain;

import com.gomdol.concert.point.domain.point.Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class PointTest {

    @Test
    void 유효한_값으로_포인트를_생성한다() {
        // given
        Long userId = 1L;
        Long balance = 10000L;

        // when
        Point point = Point.create(userId, balance);

        // then
        assertThat(point.getUserId()).isEqualTo(1L);
        assertThat(point.getBalance()).isEqualTo(10000L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1000299L, -1L, 0L})
    void 존재하지_않는_사용자로_포인트_생성시_에러를_발생시킨다(Long invalidUserId) {
        // when & then
        assertThatThrownBy(() -> Point.create(invalidUserId, 10000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID는 null이거나 0보다 작을 수 없습니다.");
    }

    @Test
    public void 음수_포인트로_생성시_에러를_발생시킨다() throws Exception {
        // when & then
        assertThatThrownBy(() -> Point.create(1L, -1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("초기 잔액은 음수일 수 없습니다");
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, Long.MAX_VALUE})
    void 경계값_userId로_포인트를_조회한다(Long userId) {
        // Given
        long expectedPoint = userId == 1L ? 1000L : 2000L; // 테스트용 포인트 값
        Point point = Point.create(userId, expectedPoint);
        // When & Then
        assertThat(point.getUserId()).isEqualTo(userId);
        assertThat(point.getBalance()).isEqualTo(expectedPoint);
    }

    @Test
    public void 사용자가_자신의_포인트를_조회한다() throws Exception {
        // given
        Point point = Point.create(1L, 10000L);
        // when & then
        assertThat(point.getBalance()).isEqualTo(10000);
        assertThat(point.getUserId()).isEqualTo(1L);
    }

    @Test
    public void 포인트가_0인_사용자가_포인트를_충전하면_잔액이_증가한다() throws Exception {
        // given
        Point point = Point.create(1L, 0L);
        // when
        point.changeBalance(10000);
        // then
        assertThat(point.getBalance()).isEqualTo(10000);
        assertThat(point.getUserId()).isEqualTo(1L);
    }

    @Test
    public void 보유_포인트가_있는_사용자가_포인트_충전을_하면_포인트가_잔액이_증가한다() throws Exception {
        // given
        Point point = Point.create(1L, 10000L);
        // when
        point.changeBalance(10000);
        // then
        assertThat(point.getBalance()).isEqualTo(20000);
        assertThat(point.getUserId()).isEqualTo(1L);
    }

    @Test
    public void 포인트_사용시_잔액이_차감된다() throws Exception {
        // given
        Point point = Point.create(1L, 10000L);
        // when
        point.usePoint(3000);
        // then
        assertThat(point.getBalance()).isEqualTo(7000);
        assertThat(point.getUserId()).isEqualTo(1L);
    }

    @Test
    public void 보유_포인트보다_사용금액이_많으면_에러가_발생한다() throws Exception {
        // given
        Point point = Point.create(1L, 3000L);
        // when & then
        assertThatThrownBy(() -> point.usePoint(10000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액이 부족합니다.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1000L, -1L, 0L})
    void userId가_잘못된_데이터로_들어오면_에러를_발생_시킨다(Long userId) {
        // when&then
        assertThatThrownBy(() -> Point.create(userId, 10000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID는 null이거나 0보다 작을 수 없습니다.");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1000L, -1L, 0L})
    void 충전_금액이_양수가_아닌_경우_예외를_발생한다(long chargeAmount) {
        // given
        Point point = Point.create(1L, 10000L);
        // when&then
        assertThatThrownBy(() -> point.changeBalance(chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전금액은 0보다 커야합니다.");
    }

    // 최대 충전 금액은 100만원으로 한다
    @ParameterizedTest
    @ValueSource(longs = {1000001L, 99999999L})
    void 충전_금액이_최대_충전_금액보다_많은_경우_예외를_발생한다(long chargeAmount) {
        // given
        Point point = Point.create(1L, 10000L);

        // when&then
        assertThatThrownBy(() -> point.changeBalance(chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 충전 금액을 초과하였습니다.");

    }

    @ParameterizedTest
    @ValueSource(longs = {100L, 9999L, 10001L, 19999L, 20001L})
    void 충전_단위에_맞지_않으면_포인트_충전_시_예외를_발생한다(long chargeAmount) {
        // given
        Point point = Point.create(1L, chargeAmount);
        // when&then
        assertThatThrownBy(() -> point.usePoint(chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최소 사용 단위보다 작은 단위입니다.");
    }
}
