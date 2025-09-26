package com.gomdol.concert.reservation.application;

import com.gomdol.concert.reservation.application.port.in.ReservationSeatCommand;
import com.gomdol.concert.reservation.application.port.out.ReservationCodeGenerator;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.application.port.out.ReservationSeatRepository;
import com.gomdol.concert.reservation.application.usecase.ReservationUseCase;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.show.domain.repository.ShowQueryRepository;
import com.gomdol.concert.venue.application.port.out.VenueSeatRepository;
import com.gomdol.concert.venue.domain.model.VenueSeat;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationUseCaseTest {

    @Mock
    private ReservationRepository reservationRepository; // 살제 예약이 완료된 정보들을 가지고 있는?

    @Mock
    private ReservationSeatRepository reservationSeatRepository; // 예약 좌석에 대한 repository

    @Mock
    private VenueSeatRepository venueSeatRepository;

    @Mock
    private ShowQueryRepository showQueryRepository;

    @Mock
    ReservationCodeGenerator reservationCodeGenerator;

    @InjectMocks
    private ReservationUseCase reservationUseCase;

    @Test
    public void 좌석_1개_예약을_성공적으로_진행한다() throws Exception {
        // given
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(1L));
        List<ReservationSeat> reservationSeats = mockOneReservationSeat();
        Reservation savedReservation = mockOneSeatReservation(reservationSeats);
        List<VenueSeat> venueSeats = mockOneVenueSeat();
        when(reservationRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(showQueryRepository.existsById(1L)).thenReturn(true);
        when(venueSeatRepository.findByIds(List.of(1L))).thenReturn(venueSeats);
        when(reservationSeatRepository.existsByShowIdAndIdIn(eq(1L), anyList())).thenReturn(false);
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
        verify(reservationSeatRepository).existsByShowIdAndIdIn(eq(1L), anyList());

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
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(1L,2L,3L));
        List<ReservationSeat> reservationSeats = mockReservationSeats();
        Reservation savedReservation = mockReservation(reservationSeats);
        List<VenueSeat> venueSeats = mockVenueSeats();
        when(reservationRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty());
        when(showQueryRepository.existsById(1L)).thenReturn(true);
        when(venueSeatRepository.findByIds(List.of(1L, 2L, 3L))).thenReturn(venueSeats);
        when(reservationSeatRepository.existsByShowIdAndIdIn(eq(1L), anyList())).thenReturn(false);
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
        verify(reservationSeatRepository).existsByShowIdAndIdIn(eq(1L), anyList());

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
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(1L,2L,3L));
        when(showQueryRepository.existsById(command.showId())).thenReturn(true);
        when(venueSeatRepository.findByIds(command.seatIds())).thenReturn(mockVenueSeats());
        when(reservationSeatRepository.existsByShowIdAndIdIn(eq(command.showId()), anyList())).thenReturn(true);
        // when && then
        assertThatThrownBy(() -> reservationUseCase.reservationSeat(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미");

        verify(reservationRepository, never()).save(any());

    }

    @Test
    public void 좌석이_존재하지_않으면_에러를_발생한다() throws Exception {
        // given
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
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID, FIXED_REQUEST_ID,1L, List.of(1L,2L,3L));
        List<ReservationSeat> reservationSeats = mockReservationSeats();
        Reservation savedReservation = mockReservation(reservationSeats);
        List<VenueSeat> venueSeats = mockVenueSeats();
        when(reservationRepository.findByRequestId(command.requestId())).thenReturn(Optional.empty(), Optional.of(savedReservation));
        when(showQueryRepository.existsById(command.showId())).thenReturn(true);
        when(venueSeatRepository.findByIds(command.seatIds())).thenReturn(venueSeats);
        when(reservationSeatRepository.existsByShowIdAndIdIn(eq(command.showId()), anyList())).thenReturn(false);
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
}
