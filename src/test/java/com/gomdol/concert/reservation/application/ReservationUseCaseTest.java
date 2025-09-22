package com.gomdol.concert.reservation.application;

import com.gomdol.concert.reservation.application.port.in.ReservationSeatCommand;
import com.gomdol.concert.reservation.application.port.out.ReservationCodeGenerator;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.application.port.out.ReservationSeatRepository;
import com.gomdol.concert.reservation.application.usecase.ReservationUseCase;
import com.gomdol.concert.reservation.domain.ReservationSeatStatus;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import com.gomdol.concert.reservation.application.port.in.ReservationResponse;
import com.gomdol.concert.show.domain.repository.ShowQueryRepository;
import com.gomdol.concert.venue.application.port.out.VenueSeatRepository;
import com.gomdol.concert.venue.domain.model.VenueSeat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private static final String FIXED_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String RESERVATION_CODE = "reservationCode";

    @Test
    public void 좌석_1개_예약을_성공적으로_진행한다() throws Exception {
        // given
        ReservationSeatCommand command = new ReservationSeatCommand(FIXED_UUID,1L, List.of(1L));
        List<ReservationSeat> reservationSeats = List.of(ReservationSeat.of(1L,1L,1L,1L,ReservationSeatStatus.HOLD));
        Reservation savedReservation = mockReservation(reservationSeats);
        when(showQueryRepository.existsById(1L)).thenReturn(true);
        when(venueSeatRepository.findByIds(List.of(1L))).thenReturn(mockVenueSeats());
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
        verify(reservationRepository).save(argThat(reservation ->
                reservation.getUserId().equals(FIXED_UUID) &&
                        reservation.getReservationCode().equals(RESERVATION_CODE) &&
                        reservation.getAmount() == 30000
        ));
    }

    private Reservation mockReservation(List<ReservationSeat> reservationSeats) {
        return Reservation.of(1L, FIXED_UUID, RESERVATION_CODE, reservationSeats, 30000, LocalDateTime.now().plusMinutes(10), null);
    }

    private List<VenueSeat> mockVenueSeats() {
        return List.of(
                VenueSeat.create(1L, "A", 1, 10000L),
                VenueSeat.create( 1L, "A", 2, 10000L),
                VenueSeat.create( 1L, "A", 3, 10000L)
        );
    }
}
