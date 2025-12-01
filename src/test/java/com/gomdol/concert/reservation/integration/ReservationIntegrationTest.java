package com.gomdol.concert.reservation.integration;

import com.gomdol.concert.common.TestContainerConfig;
import com.gomdol.concert.common.TestDataFactory;
import com.gomdol.concert.concert.infra.persistence.entitiy.ConcertEntity;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort.ReservationSeatCommand;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.infra.persistence.entity.ReservationSeatEntity;
import com.gomdol.concert.reservation.infra.persistence.ReservationJpaRepository;
import com.gomdol.concert.reservation.infra.persistence.ReservationSeatJpaRepository;
import com.gomdol.concert.show.infra.persistence.entity.ShowEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueEntity;
import com.gomdol.concert.venue.infra.persistence.entity.VenueSeatEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.gomdol.concert.common.FixedField.FIXED_UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("좌석 예약 통합 테스트")
@Transactional
@Import(TestContainerConfig.class)
class ReservationIntegrationTest {

    @Autowired
    private ReservationSeatPort reservationSeatPort;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private ReservationSeatJpaRepository reservationSeatJpaRepository;

    @Autowired
    private TestDataFactory testDataFactory;

    private Long testShowId;
    private Long testSeatId1;
    private Long testSeatId2;

    @BeforeEach
    void setUp() {
        // 기존 예약 데이터 초기화
        reservationSeatJpaRepository.deleteAll();
        reservationJpaRepository.deleteAll();

        // 테스트 데이터 생성
        VenueEntity venue = testDataFactory.createVenue("Test Venue", "Seoul", 100);
        VenueSeatEntity seat1 = testDataFactory.createVenueSeat(venue, "A", 1, 10000L);
        VenueSeatEntity seat2 = testDataFactory.createVenueSeat(venue, "A", 2, 10000L);

        ConcertEntity concert = testDataFactory.createConcert("Test Concert", venue);
        ShowEntity show = testDataFactory.createShow(concert, LocalDateTime.now().plusDays(7), 100);

        testShowId = show.getId();
        testSeatId1 = seat1.getId();
        testSeatId2 = seat2.getId();
    }

    @Test
    @DisplayName("좌석 예약 성공 - HOLD 상태로 생성")
    void 좌석_예약_성공() {
        // given
        String requestId = UUID.randomUUID().toString();
        List<Long> seatIds = List.of(testSeatId1, testSeatId2);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, requestId, testShowId, seatIds);

        // when
        ReservationResponse response = reservationSeatPort.reservationSeat(command);

        // then
        assertThat(response).isNotNull();
        assertThat(response.reservationCode()).isNotNull();
        assertThat(response.reservationId()).isNotNull();

        // DB 확인
        Reservation savedReservation = reservationRepository.findById(response.reservationId()).orElseThrow();
        assertThat(savedReservation.getReservationSeats()).hasSize(2);
        assertThat(savedReservation.getUserId()).isEqualTo(FIXED_UUID);
        assertThat(savedReservation.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(savedReservation.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(11));
    }

    @Test
    @DisplayName("멱등성 테스트 - 동일한 requestId로 재요청 시 기존 예약 반환")
    void 멱등성_테스트() {
        // given
        String requestId = UUID.randomUUID().toString();
        List<Long> seatIds = List.of(testSeatId1);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, requestId, testShowId, seatIds);

        // when - 첫 번째 예약
        ReservationResponse firstResponse = reservationSeatPort.reservationSeat(command);

        // when - 동일한 requestId로 재요청
        ReservationResponse secondResponse = reservationSeatPort.reservationSeat(command);

        // then - 동일한 예약이 반환되어야 함
        assertThat(firstResponse.reservationCode()).isEqualTo(secondResponse.reservationCode());
        assertThat(firstResponse.requestId()).isEqualTo(secondResponse.requestId());

        // Repository로 조회하여 같은 예약임을 확인
        Reservation reservation = reservationRepository.findById(firstResponse.reservationId()).orElseThrow();
        assertThat(reservation.getReservationCode()).isEqualTo(firstResponse.reservationCode());
        assertThat(reservation.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("존재하지 않는 공연 ID로 예약 시 예외 발생")
    void 존재하지_않는_공연_예약_실패() {
        // given
        Long nonExistentShowId = 99999L;
        String requestId = UUID.randomUUID().toString();
        List<Long> seatIds = List.of(testSeatId1);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, requestId, nonExistentShowId, seatIds);

        // when & then
        assertThatThrownBy(() -> reservationSeatPort.reservationSeat(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공연이 존재하지 않습니다");
    }

    @Test
    @DisplayName("존재하지 않는 좌석 ID로 예약 시 예외 발생")
    void 존재하지_않는_좌석_예약_실패() {
        // given
        String requestId = UUID.randomUUID().toString();
        List<Long> seatIds = List.of(99999L); // 존재하지 않는 좌석 ID
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, requestId, testShowId, seatIds);

        // when & then
        assertThatThrownBy(() -> reservationSeatPort.reservationSeat(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("좌석이 존재하지 않습니다");
    }

    @Test
    @DisplayName("이미 예약된 좌석으로 예약 시 예외 발생")
    void 이미_예약된_좌석_예약_실패() {
        // given - 먼저 좌석을 예약
        String firstRequestId = UUID.randomUUID().toString();
        List<Long> seatIds = List.of(testSeatId1);
        ReservationSeatCommand firstCommand = new ReservationSeatCommand(FIXED_UUID, firstRequestId, testShowId, seatIds);
        reservationSeatPort.reservationSeat(firstCommand);

        // when - 다른 사용자가 동일한 좌석 예약 시도
        String secondRequestId = UUID.randomUUID().toString();
        ReservationSeatCommand secondCommand = new ReservationSeatCommand("otherUser", secondRequestId, testShowId, seatIds);

        // then
        assertThatThrownBy(() -> reservationSeatPort.reservationSeat(secondCommand))
                .isInstanceOf(RuntimeException.class);
    }

    // TODO: 예약 조회 기능 추가 필요
//    @Test
//    @DisplayName("예약 코드로 예약 조회")
//    void 예약_코드로_조회() {
//        // given
//        String reservationCode = UUID.randomUUID().toString();
//        List<Long> seatIds = List.of(testSeatId1, testSeatId2);
//        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, requestId, testShowId, seatIds);
//        ReservationResponse createdReservation = reservationSeatPort.reservationSeat(command);
//
//        // when
//        Reservation foundReservation = reservationRepository.findByReservationCode(requestId).orElseThrow();
//
//        // then
//        assertThat(foundReservation).isNotNull();
//        assertThat(foundReservation.getUserId()).isEqualTo(FIXED_UUID);
//        assertThat(foundReservation.getReservationCode()).isEqualTo(createdReservation.reservationCode());
//        assertThat(foundReservation.getReservationSeats()).hasSize(2);
//    }

    @Test
    @DisplayName("예약된 좌석 상태가 HOLD인지 확인")
    void 예약된_좌석_상태_확인() {
        // given
        String requestId = UUID.randomUUID().toString();
        List<Long> seatIds = List.of(testSeatId1);

        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, requestId, testShowId, seatIds);

        // when
        reservationSeatPort.reservationSeat(command);

        // then - DB에서 직접 확인
        List<ReservationSeatEntity> reservedSeats = reservationSeatJpaRepository.findAll();
        assertThat(reservedSeats).isNotEmpty();

        ReservationSeatEntity reservedSeat = reservedSeats.stream()
                .filter(rs -> rs.getSeatId().equals(testSeatId1))
                .findFirst()
                .orElseThrow();

        assertThat(reservedSeat.getStatus()).isEqualTo(ReservationSeatStatus.HOLD);
        assertThat(reservedSeat.getShowId()).isEqualTo(testShowId);
    }
}