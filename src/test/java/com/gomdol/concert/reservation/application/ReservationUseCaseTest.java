package com.gomdol.concert.reservation.application;

import com.gomdol.concert.reservation.application.port.in.ReservationSeatPort.ReservationSeatCommand;
import com.gomdol.concert.reservation.application.port.out.ReservationCodeGenerator;
import com.gomdol.concert.reservation.application.port.out.ReservationPolicyProvider;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.application.usecase.ReservationSeatUseCase;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.show.application.port.out.ShowQueryRepository;
import com.gomdol.concert.venue.application.port.out.VenueSeatRepository;
import com.gomdol.concert.venue.domain.model.VenueSeat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.gomdol.concert.common.FixedField.*;
import static com.gomdol.concert.common.ReservationTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationUseCaseTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private VenueSeatRepository venueSeatRepository;

    @Mock
    private ShowQueryRepository showQueryRepository;

    @Mock
    private ReservationCodeGenerator reservationCodeGenerator;

    @Mock
    private ReservationPolicyProvider policyProvider;

    @InjectMocks
    private ReservationSeatUseCase reservationUseCase;

    @Test
    public void 좌석_1개_예약을_성공적으로_진행한다() throws Exception {
        // given
        given(policyProvider.holdMinutes()).willReturn(10);
        given(policyProvider.maxSeatsPerReservation()).willReturn(4);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(1L));
        List<ReservationSeat> reservationSeats = mockOneReservationSeat();
        Reservation savedReservation = mockOneSeatReservation(reservationSeats);
        List<VenueSeat> venueSeats = mockOneVenueSeat();
        when(reservationRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(showQueryRepository.existsById(1L)).thenReturn(true);
        when(venueSeatRepository.findByIds(List.of(1L))).thenReturn(venueSeats);
        when(reservationCodeGenerator.newReservationCode()).thenReturn(RESERVATION_CODE);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);
        // when
        ReservationResponse result = reservationUseCase.reservationSeat(command);
        // then
        assertThat(result).isNotNull();
        assertThat(result.reservationId()).isEqualTo(1L);
        assertThat(result.expiredAt()).isAfter(LocalDateTime.now());

        verify(showQueryRepository).existsById(1L);
        verify(venueSeatRepository).findByIds(List.of(1L));
//        verify(reservationSeatRepository).existsByShowIdAndIdIn(eq(1L), anyList());

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation saved = captor.getValue();
        assertAll(
                () -> assertThat(saved.getUserId()).isEqualTo(command.userId()),
                () -> assertThat(saved.getReservationCode()).isEqualTo(RESERVATION_CODE),
                () -> assertThat(saved.getRequestId()).isEqualTo(FIXED_REQUEST_ID),
                () -> assertThat(saved.getAmount()).isEqualTo(venueSeats.stream().mapToLong(VenueSeat::getPrice).sum()),
                () -> assertThat(saved.getReservationSeats().size()).isEqualTo(command.seatIds().size())
        );
    }

    @Test
    public void 복수의_좌석_예약을_성공적으로_진행한다() throws Exception {
        // given
        given(policyProvider.holdMinutes()).willReturn(10);
        given(policyProvider.maxSeatsPerReservation()).willReturn(4);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(1L,2L,3L));
        List<ReservationSeat> reservationSeats = mockReservationSeats();
        Reservation savedReservation = mockReservation(reservationSeats);
        List<VenueSeat> venueSeats = mockVenueSeats();
        when(reservationRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(showQueryRepository.existsById(1L)).thenReturn(true);
        when(venueSeatRepository.findByIds(List.of(1L, 2L, 3L))).thenReturn(venueSeats);
        when(reservationCodeGenerator.newReservationCode()).thenReturn(RESERVATION_CODE);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);
        // when
        ReservationResponse result = reservationUseCase.reservationSeat(command);
        // then
        assertThat(result).isNotNull();
        assertThat(result.reservationId()).isEqualTo(1L);
        assertThat(result.expiredAt()).isAfter(LocalDateTime.now());

        verify(showQueryRepository).existsById(1L);
        verify(venueSeatRepository).findByIds(List.of(1L, 2L, 3L));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation saved = captor.getValue();
        assertAll(
                () -> assertThat(saved.getUserId()).isEqualTo(command.userId()),
                () -> assertThat(saved.getReservationCode()).isEqualTo(RESERVATION_CODE),
                () -> assertThat(saved.getRequestId()).isEqualTo(FIXED_REQUEST_ID),
                () -> assertThat(saved.getAmount()).isEqualTo(venueSeats.stream().mapToLong(VenueSeat::getPrice).sum()),
                () -> assertThat(saved.getReservationSeats().size()).isEqualTo(command.seatIds().size())
        );
    }

    @Test
    public void 존재하지_않는_공연ID가_들어오면_예외를_발생시킨다 () throws Exception {
        // given
        given(policyProvider.maxSeatsPerReservation()).willReturn(4);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,999L, List.of(1L,2L,3L));
        when(showQueryRepository.existsById(command.showId())).thenReturn(false);
        // when && then
        assertThatThrownBy(() -> reservationUseCase.reservationSeat(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공연이 존재하지");
    }

    @Test
    public void 이미_예약된_좌석이면_에러를_발생한다() throws Exception {
        // given
        given(policyProvider.maxSeatsPerReservation()).willReturn(4);
        given(policyProvider.holdMinutes()).willReturn(10);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(1L,2L,3L));
        when(reservationRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(showQueryRepository.existsById(command.showId())).thenReturn(true);
        when(venueSeatRepository.findByIds(command.seatIds())).thenReturn(mockVenueSeats());
        when(reservationCodeGenerator.newReservationCode()).thenReturn(RESERVATION_CODE);
        when(reservationRepository.save(any(Reservation.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"));

        // when && then
        assertThatThrownBy(() -> reservationUseCase.reservationSeat(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 선택된 좌석입니다.");

    }

    @Test
    public void 좌석이_존재하지_않으면_에러를_발생한다() throws Exception {
        // given
        given(policyProvider.maxSeatsPerReservation()).willReturn(4);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(100L));
        when(showQueryRepository.existsById(command.showId())).thenReturn(true);
        when(venueSeatRepository.findByIds(command.seatIds())).thenReturn(List.of());
        assertThatThrownBy(() -> reservationUseCase.reservationSeat(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("좌석이 존재하지");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    public void 동일한_멱등키로_요청이들어오면_동일한_결과를_리턴한다() throws Exception {
        // given
        given(policyProvider.holdMinutes()).willReturn(10);
        given(policyProvider.maxSeatsPerReservation()).willReturn(4);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(1L,2L,3L));
        List<ReservationSeat> reservationSeats = mockReservationSeats();
        Reservation savedReservation = mockReservation(reservationSeats);
        List<VenueSeat> venueSeats = mockVenueSeats();

        when(reservationRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty(), Optional.of(savedReservation));
        when(showQueryRepository.existsById(command.showId())).thenReturn(true);
        when(venueSeatRepository.findByIds(command.seatIds())).thenReturn(venueSeats);
        when(reservationCodeGenerator.newReservationCode()).thenReturn(RESERVATION_CODE);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        // when
        ReservationResponse result = reservationUseCase.reservationSeat(command);
        ReservationResponse secondResult = reservationUseCase.reservationSeat(command);

        // then
        verify(reservationRepository, times(1)).save(any()); // 한 번만 저장
        assertThat(secondResult.reservationId()).isEqualTo(1L);

        assertThat(result).isNotNull();
        assertThat(result.reservationId()).isEqualTo(1L);
        assertThat(result.expiredAt()).isAfter(LocalDateTime.now());

        assertThat(secondResult).isNotNull();
        assertThat(secondResult.reservationId()).isEqualTo(1L);
        assertThat(secondResult.expiredAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("예약 좌석이 5자리 이상이면 에러를 발생한다")
    public void reserveSeats_shouldThrowException_whenSeatCountExceedsLimit() {
        // given
        given(policyProvider.maxSeatsPerReservation()).willReturn(4);
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(100L, 101L, 102L, 103L, 104L));

        // when & then
        assertThatThrownBy(() -> reservationUseCase.reservationSeat(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 4개 좌석까지 예약 가능합니다.");

        verify(reservationRepository, never()).save(any());
    }
}
