package com.gomdol.concert.point.integration;

import com.gomdol.concert.common.TestContainerConfig;
import com.gomdol.concert.point.application.port.in.GetPointBalancePort;
import com.gomdol.concert.point.application.port.in.SavePointPort;
import com.gomdol.concert.point.application.port.out.PointHistoryRepository;
import com.gomdol.concert.point.application.port.out.PointRepository;
import com.gomdol.concert.point.domain.model.Point;
import com.gomdol.concert.point.domain.model.PointHistory;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.infra.persistence.PointHistoryJpaRepository;
import com.gomdol.concert.point.infra.persistence.PointJpaRepository;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("포인트 통합 테스트")
@Import(TestContainerConfig.class)
class PointIntegrationTest {

    @Autowired
    private SavePointPort savePointPort;

    @Autowired
    private GetPointBalancePort getPointBalancePort;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private PointHistoryJpaRepository pointHistoryJpaRepository;

    @BeforeEach
    void setUp() {
        // 포인트 데이터 초기화
        pointHistoryJpaRepository.deleteAll();
        pointJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void 포인트_충전_성공() {
        // given
        String requestId = UUID.randomUUID().toString();
        long chargeAmount = 10000L;
        PointRequest request = new PointRequest(requestId, chargeAmount, UseType.CHARGE);

        // when
        PointResponse response = savePointPort.savePoint(FIXED_UUID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(FIXED_UUID);
        assertThat(response.balance()).isEqualTo(chargeAmount);

        // DB 확인
        Point savedPoint = pointRepository.findByUserIdWithLock(FIXED_UUID).orElseThrow();
        assertThat(savedPoint.getBalance()).isEqualTo(chargeAmount);

        // 히스토리 확인
        PointHistory history = pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, requestId).orElseThrow();
        assertThat(history.getAmount()).isEqualTo(chargeAmount);
        assertThat(history.getUseType()).isEqualTo(UseType.CHARGE);
        assertThat(history.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("포인트 여러 번 충전 - 잔액 누적")
    void 포인트_여러번_충전() {
        // given
        String requestId1 = UUID.randomUUID().toString();
        String requestId2 = UUID.randomUUID().toString();
        long chargeAmount1 = 10000L;
        long chargeAmount2 = 5000L;

        // when
        savePointPort.savePoint(FIXED_UUID, new PointRequest(requestId1, chargeAmount1, UseType.CHARGE));
        savePointPort.savePoint(FIXED_UUID, new PointRequest(requestId2, chargeAmount2, UseType.CHARGE));

        // then
        Point point = pointRepository.findByUserIdWithLock(FIXED_UUID).orElseThrow();
        assertThat(point.getBalance()).isEqualTo(chargeAmount1 + chargeAmount2);

        // 히스토리 확인 - requestId로 개별 확인
        assertThat(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, requestId1)).isPresent();
        assertThat(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, requestId2)).isPresent();
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void 포인트_사용_성공() {
        // given - 먼저 포인트 충전
        String chargeRequestId = UUID.randomUUID().toString();
        long chargeAmount = 10000L;
        savePointPort.savePoint(FIXED_UUID, new PointRequest(chargeRequestId, chargeAmount, UseType.CHARGE));

        // when - 포인트 사용
        String useRequestId = UUID.randomUUID().toString();
        long useAmount = 3000L;
        PointRequest useRequest = new PointRequest(useRequestId, useAmount, UseType.USE);
        PointResponse response = savePointPort.savePoint(FIXED_UUID, useRequest);

        // then
        assertThat(response.balance()).isEqualTo(chargeAmount - useAmount);

        // DB 확인
        Point point = pointRepository.findByUserIdWithLock(FIXED_UUID).orElseThrow();
        assertThat(point.getBalance()).isEqualTo(chargeAmount - useAmount);

        // 히스토리 확인 - 사용 내역 확인
        PointHistory useHistory = pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, useRequestId).orElseThrow();
        assertThat(useHistory.getAmount()).isEqualTo(-useAmount);  // USE 타입은 음수로 저장됨
        assertThat(useHistory.getUseType()).isEqualTo(UseType.USE);
        assertThat(useHistory.getRequestId()).isEqualTo(useRequestId);
    }

    @Test
    @DisplayName("포인트 잔액 부족 시 사용 실패")
    void 포인트_잔액_부족_사용_실패() {
        // given - 포인트 충전
        String chargeRequestId = UUID.randomUUID().toString();
        long chargeAmount = 5000L;
        savePointPort.savePoint(FIXED_UUID, new PointRequest(chargeRequestId, chargeAmount, UseType.CHARGE));

        // when & then - 잔액보다 많은 금액 사용 시도
        String useRequestId = UUID.randomUUID().toString();
        long useAmount = 10000L;
        PointRequest useRequest = new PointRequest(useRequestId, useAmount, UseType.USE);

        assertThatThrownBy(() -> savePointPort.savePoint(FIXED_UUID, useRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액이 부족합니다");

        // 잔액 변경 없음 확인
        Point point = pointRepository.findByUserIdWithLock(FIXED_UUID).orElseThrow();
        assertThat(point.getBalance()).isEqualTo(chargeAmount);
    }

    @Test
    @DisplayName("포인트 충전 멱등성 테스트 - 동일 requestId 재요청")
    void 포인트_충전_멱등성() {
        // given
        String requestId = UUID.randomUUID().toString();
        long chargeAmount = 10000L;
        PointRequest request = new PointRequest(requestId, chargeAmount, UseType.CHARGE);

        // when - 동일한 requestId로 두 번 요청
        PointResponse firstResponse = savePointPort.savePoint(FIXED_UUID, request);
        PointResponse secondResponse = savePointPort.savePoint(FIXED_UUID, request);

        // then - 잔액이 중복 충전되지 않음
        assertThat(firstResponse.balance()).isEqualTo(secondResponse.balance());
        assertThat(secondResponse.balance()).isEqualTo(chargeAmount);

        // DB 확인 - 히스토리도 1건만 생성
        PointHistory history = pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, requestId).orElseThrow();
        assertThat(history.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("포인트 사용 멱등성 테스트 - 동일 requestId 재요청")
    void 포인트_사용_멱등성() {
        // given - 먼저 포인트 충전
        String chargeRequestId = UUID.randomUUID().toString();
        savePointPort.savePoint(FIXED_UUID, new PointRequest(chargeRequestId, 10000L, UseType.CHARGE));

        // when - 동일한 requestId로 두 번 사용 요청
        String useRequestId = UUID.randomUUID().toString();
        long useAmount = 3000L;
        PointRequest useRequest = new PointRequest(useRequestId, useAmount, UseType.USE);

        PointResponse firstResponse = savePointPort.savePoint(FIXED_UUID, useRequest);
        PointResponse secondResponse = savePointPort.savePoint(FIXED_UUID, useRequest);

        // then - 잔액이 중복 차감되지 않음
        assertThat(firstResponse.balance()).isEqualTo(secondResponse.balance());
        assertThat(secondResponse.balance()).isEqualTo(10000L - useAmount);

        // DB 확인 - 히스토리 개별 확인
        assertThat(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, chargeRequestId)).isPresent();
        assertThat(pointHistoryRepository.findByUserIdAndRequestId(FIXED_UUID, useRequestId)).isPresent();
    }

    @Test
    @DisplayName("포인트 조회 - 잔액 확인")
    void 포인트_조회() {
        // given - 포인트 충전
        String requestId = UUID.randomUUID().toString();
        long chargeAmount = 15000L;
        savePointPort.savePoint(FIXED_UUID, new PointRequest(requestId, chargeAmount, UseType.CHARGE));

        // when
        PointResponse response = getPointBalancePort.getPoint(FIXED_UUID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(FIXED_UUID);
        assertThat(response.balance()).isEqualTo(chargeAmount);
    }

    @Test
    @DisplayName("포인트 조회 - 포인트 없는 사용자는 0원 반환")
    void 포인트_없는_사용자_조회() {
        // given - 포인트 내역 없는 새 사용자
        String newUserId = UUID.randomUUID().toString();

        // when
        PointResponse response = getPointBalancePort.getPoint(newUserId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(newUserId);
        assertThat(response.balance()).isEqualTo(0L);
    }


    @Test
    @DisplayName("잘못된 금액 - 0원 충전 시 예외 발생")
    void 잘못된_금액_충전_실패() {
        // given
        String requestId = UUID.randomUUID().toString();
        PointRequest request = new PointRequest(requestId, 0L, UseType.CHARGE);

        // when & then
        assertThatThrownBy(() -> savePointPort.savePoint(FIXED_UUID, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("잘못된 금액 - 음수 충전 시 예외 발생")
    void 음수_금액_충전_실패() {
        // given
        String requestId = UUID.randomUUID().toString();
        PointRequest request = new PointRequest(requestId, -5000L, UseType.CHARGE);

        // when & then
        assertThatThrownBy(() -> savePointPort.savePoint(FIXED_UUID, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("포인트 충전 후 전액 사용")
    void 포인트_전액_사용() {
        // given - 포인트 충전
        String chargeRequestId = UUID.randomUUID().toString();
        long chargeAmount = 10000L;
        savePointPort.savePoint(FIXED_UUID, new PointRequest(chargeRequestId, chargeAmount, UseType.CHARGE));

        // when - 전액 사용
        String useRequestId = UUID.randomUUID().toString();
        savePointPort.savePoint(FIXED_UUID, new PointRequest(useRequestId, chargeAmount, UseType.USE));

        // then
        Point point = pointRepository.findByUserIdWithLock(FIXED_UUID).orElseThrow();
        assertThat(point.getBalance()).isEqualTo(0L);
    }
}