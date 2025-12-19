package com.gomdol.concert.reservation.application.eventhandler;

import com.gomdol.concert.reservation.domain.event.ReservationCompletedEvent;
import com.gomdol.concert.reservation.infra.external.DataPlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 완료 이벤트 핸들러
 * 트랜잭션 커밋 후 비동기로 데이터 플랫폼에 데이터 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventHandler {

    private final DataPlatformClient dataPlatformClient;

    /**
     * 예약 완료 이벤트 처리
     * - 트랜잭션 커밋 후에 실행 (AFTER_COMMIT)
     * - 비동기로 실행하여 응답 시간에 영향 없음
     * - 실패 시 DataPlatformClient의 @Retryable에 의해 자동 재시도
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationCompleted(ReservationCompletedEvent event) {
        log.info("예약 완료 이벤트 수신 - reservationId: {}, userId: {}",
                event.getReservationId(), event.getUserId());

        try {
            // 이벤트를 DTO로 변환
            DataPlatformClient.ReservationDataDto data = convertToDto(event);

            // 데이터 플랫폼으로 전송 (재시도 포함)
            dataPlatformClient.sendReservationData(data);

            log.info("예약 데이터 전송 성공 - reservationId: {}", event.getReservationId());

        } catch (Exception e) {
            // 모든 재시도가 실패한 경우
            log.error("예약 데이터 전송 최종 실패 - reservationId: {}, 수동 재처리 필요",
                    event.getReservationId(), e);

            // TODO: 실패한 이벤트를 별도 테이블이나 Queue에 저장하여 나중에 재처리
            // 예: deadLetterQueue.save(event) 또는 failedEventRepository.save(event)
        }
    }

    private DataPlatformClient.ReservationDataDto convertToDto(ReservationCompletedEvent event) {
        List<DataPlatformClient.ReservationDataDto.SeatData> seatDataList = event.getSeats().stream()
                .map(seat -> new DataPlatformClient.ReservationDataDto.SeatData(
                        seat.seatId(),
                        null,  // seatNumber는 이벤트에 포함되지 않음
                        seat.showId(),
                        seat.price()
                ))
                .toList();

        return new DataPlatformClient.ReservationDataDto(
                event.getReservationId(),
                event.getUserId(),
                event.getReservationCode(),
                seatDataList,
                event.getTotalAmount(),
                event.getConfirmedAt(),
                LocalDateTime.now() // 전송 시각
        );
    }
}
