package com.gomdol.concert.reservation.infra.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gomdol.concert.common.application.outbox.OutboxRepository;
import com.gomdol.concert.common.domain.outbox.OutboxEvent;
import com.gomdol.concert.concert.application.port.out.ConcertRepository;
import com.gomdol.concert.concert.domain.event.RankingUpdateRequestedEvent;
import com.gomdol.concert.reservation.application.port.out.ReservationRepository;
import com.gomdol.concert.reservation.application.service.ReservationCommandService;
import com.gomdol.concert.reservation.domain.command.CancelSeatsCommand;
import com.gomdol.concert.reservation.domain.command.ConfirmSeatsCommand;
import com.gomdol.concert.reservation.domain.event.ReservationCompletedEvent;
import com.gomdol.concert.reservation.domain.event.SeatsCancelledEvent;
import com.gomdol.concert.reservation.domain.event.SeatsConfirmedEvent;
import com.gomdol.concert.reservation.domain.model.Reservation;
import com.gomdol.concert.reservation.domain.model.ReservationSeat;
import com.gomdol.concert.show.application.port.out.ShowRepository;
import com.gomdol.concert.show.domain.model.Show;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.gomdol.concert.reservation.domain.event.SeatsCancelledEvent.*;

/**
 * Reservation Service용 Saga Consumer
 *
 * 수신하는 Command:
 * 1. ConfirmSeatsCommand (from Payment Service) - 좌석 확정
 * 2. CancelSeatsCommand (from Payment Service) - 좌석 취소 (보상 트랜잭션)
 *
 * 발행하는 Event:
 * 1. SeatsConfirmedEvent - 좌석 확정 결과
 * 2. SeatsCancelledEvent - 좌석 취소 결과
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationSagaConsumer {

    private final ReservationCommandService reservationCommandService;
    private final ReservationRepository reservationRepository;
    private final ShowRepository showRepository;
    private final ConcertRepository concertRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Qualifier("reservationOutboxRepository")
    private final OutboxRepository reservationOutboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${kafka.topics.seats-confirmed-event}")
    private String seatsConfirmedEventTopic;

    @Value("${kafka.topics.seats-cancelled-event}")
    private String seatsCancelledEventTopic;

    /**
     * ConfirmSeatsCommand 수신 처리
     *
     * 하나의 트랜잭션에서:
     * 1. 좌석 확정 처리 (Reservation 상태 업데이트)
     * 2. SeatsConfirmedEvent를 Reservation Outbox에 저장
     */
    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.confirm-seats-command}",
            groupId = "${kafka.consumer.reservation-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeConfirmSeatsCommand(
            @Payload ConfirmSeatsCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("=== [Reservation Service] ConfirmSeatsCommand 수신 - paymentId={}, reservationId={}, offset={} ===", command.getPaymentId(), command.getReservationId(), offset);

        try {
            // 좌석 확정 처리
            reservationCommandService.confirmSeats(command.getReservationId());
            log.info("[Reservation Service] 좌석 확정 완료 - reservationId={}", command.getReservationId());

            // 예약 정보 조회 (이벤트 발행용)
            Reservation reservation = reservationRepository.findById(command.getReservationId())
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));

            // 성공 이벤트를 Reservation Outbox에 저장
            SeatsConfirmedEvent successEvent = SeatsConfirmedEvent.success(command.getPaymentId(), command.getReservationId(), command.getUserId(), command.getRequestId());
            saveOutboxEvent(command.getReservationId().toString(), "SeatsConfirmedEvent", seatsConfirmedEventTopic, successEvent);

            // 로컬 이벤트 발행
            publishLocalEvents(command.getReservationId(), reservation);

            acknowledgment.acknowledge();
            log.info("[Reservation Service] SeatsConfirmedEvent(success) Outbox 저장 및 로컬 이벤트 발행 완료");
        } catch (IllegalStateException e) {
            // 좌석 확정 실패 (이미 확정됨, 만료됨 등)
            log.warn("[Reservation Service] 좌석 확정 실패 - reservationId={}, reason={}", command.getReservationId(), e.getMessage());
            // 실패 이벤트를 Reservation Outbox에 저장
            SeatsConfirmedEvent failureEvent = SeatsConfirmedEvent.failure(command.getPaymentId(), command.getReservationId(), command.getUserId(), command.getRequestId(), e.getMessage());
            saveOutboxEvent(command.getReservationId().toString(), "SeatsConfirmedEvent", seatsConfirmedEventTopic, failureEvent);
            acknowledgment.acknowledge();
            log.info("[Reservation Service] SeatsConfirmedEvent(failure) Outbox 저장 완료");
        } catch (Exception e) {
            log.error("[Reservation Service] ConfirmSeatsCommand 처리 중 예외 발생", e);
            throw e;  // 재시도
        }
    }

    /**
     * CancelSeatsCommand 수신 처리 (보상 트랜잭션)
     *
     * 하나의 트랜잭션에서:
     * 1. 좌석 취소 처리 (Reservation 취소, 좌석 해제)
     * 2. SeatsCancelledEvent를 Reservation Outbox에 저장
     */
    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.cancel-seats-command}",
            groupId = "${kafka.consumer.reservation-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCancelSeatsCommand(
            @Payload CancelSeatsCommand command,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("=== [Reservation Service] CancelSeatsCommand 수신 (보상 트랜잭션) - paymentId={}, reservationId={}, offset={} ===", command.paymentId(), command.reservationId(), offset);

        try {
            // 좌석 취소 처리
            reservationCommandService.cancelSeats(command.reservationId());
            log.info("[Reservation Service] 좌석 취소 완료 - reservationId={}, reason={}", command.reservationId(), command.reason());

            // 성공 이벤트를 Reservation Outbox에 저장
            SeatsCancelledEvent successEvent = success(command.paymentId(), command.reservationId(), command.userId(), command.requestId());
            saveOutboxEvent(command.reservationId().toString(), "SeatsCancelledEvent", seatsCancelledEventTopic, successEvent);
            acknowledgment.acknowledge();
            log.info("[Reservation Service] SeatsCancelledEvent(success) Outbox 저장 완료");
        } catch (Exception e) {
            log.error("[Reservation Service] CancelSeatsCommand 처리 중 예외 발생 - 취소 실패는 심각한 문제!", e);

            // 취소 실패 이벤트 발행 (수동 처리 필요)
            SeatsCancelledEvent failureEvent = failure(command.paymentId(), command.reservationId(), command.userId(), command.requestId(), e.getMessage());
            saveOutboxEvent(command.reservationId().toString(), "SeatsCancelledEvent", seatsCancelledEventTopic, failureEvent);
            acknowledgment.acknowledge();
            log.error("[Reservation Service] SeatsCancelledEvent(failure) Outbox 저장 완료 - 수동 처리 필요!");
        }
    }

    private void saveOutboxEvent(String aggregateId, String eventType, String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.create("RESERVATION", aggregateId, eventType, topic, payload);
            reservationOutboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("[Reservation Outbox] 이벤트 직렬화 실패", e);
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }

    /**
     * 로컬 이벤트 발행
     * 1. ReservationCompletedEvent (Mock API 전송용) - 상세 정보 포함
     * 2. RankingUpdateRequestedEvent (랭킹 업데이트용) - 경량 이벤트
     */
    private void publishLocalEvents(Long reservationId, Reservation reservation) {
        // 1. ReservationCompletedEvent 발행 (Mock API용)
        // ReservationSeat에 price가 포함되어 있으므로 도메인 모델에서 직접 변환 가능
        var seatInfoList = reservation.getReservationSeats().stream()
                .map(rs -> ReservationCompletedEvent.SeatInfo.of(
                        rs.getSeatId(),
                        rs.getShowId(),
                        rs.getPrice()
                ))
                .toList();

        ReservationCompletedEvent reservationEvent = ReservationCompletedEvent.of(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getReservationCode(),
                seatInfoList,
                reservation.getAmount(),
                reservation.getConfirmedAt()
        );
        eventPublisher.publishEvent(reservationEvent);
        log.info("[Reservation Service] ReservationCompletedEvent 발행 완료 (로컬) - reservationId={}", reservationId);

        // 2. RankingUpdateRequestedEvent 발행 (랭킹 업데이트용)
        if (!reservation.getReservationSeats().isEmpty()) {
            try {
                Long showId = reservation.getReservationSeats().stream()
                        .findFirst()
                        .map(ReservationSeat::getShowId)
                        .orElseThrow(() -> new IllegalStateException("예약 좌석이 없습니다."));
                int seatCount = reservation.getReservationSeats().size();
                Show show = showRepository.findById(showId)
                        .orElseThrow(() -> new IllegalArgumentException("공연을 찾을 수 없습니다. showId=" + showId));

                RankingUpdateRequestedEvent rankingEvent = RankingUpdateRequestedEvent.of(
                        reservationId,
                        show.getConcertId(),
                        show.getConcertTitle(),
                        show.getTotalSeats(),        // totalSeats (전체 좌석 수)
                        show.getReservedSeats(),     // reservedSeats (예약된 좌석 수)
                        seatCount                     // seatCount (이번 예약 좌석 수)
                );
                eventPublisher.publishEvent(rankingEvent);
                log.info("RankingUpdateRequestedEvent 발행 완료  - reservationId={}, concertId={}, title={}, seatCount={}",
                        reservationId, show.getConcertId(), show.getConcertTitle(), seatCount);
            } catch (Exception e) {
                log.error("RankingUpdateRequestedEvent 발행 실패 - reservationId={}, error={}", reservationId, e.getMessage(), e);
                // 랭킹 업데이트 실패해도 예약은 유지
            }
        }
    }
}
