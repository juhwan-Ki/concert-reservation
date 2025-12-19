package com.gomdol.concert.reservation.infra.kafka.consumer;

import com.gomdol.concert.reservation.domain.event.ReservationCompletedEvent;
import com.gomdol.concert.reservation.infra.external.DataPlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka에서 예약 완료 이벤트를 소비하여 데이터 플랫폼으로 전송하는 Consumer
 * 실패 시 자동 재시도 및 DLT(Dead Letter Topic) 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventConsumer {

    private final DataPlatformClient dataPlatformClient;

    /**
     * 예약 완료 이벤트 소비 및 데이터 플랫폼 전송
     * - 실패 시 최대 3회 재시도 (1초 간격)
     * - 모든 재시도 실패 시 DLT로 이동
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000L),
            autoCreateTopics = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt",
            include = {Exception.class}
    )
    @KafkaListener(
            topics = "${kafka.topics.reservation-completed}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload ReservationCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Kafka 메시지 수신 - topic: {}, partition: {}, offset: {}, reservationId: {}", topic, partition, offset, event.getReservationId());

        try {
            // 이벤트를 DTO로 변환
            DataPlatformClient.ReservationDataDto data = convertToDto(event);

            // 데이터 플랫폼으로 전송
            dataPlatformClient.sendReservationData(data);

            // 성공 시 수동 커밋
            acknowledgment.acknowledge();

            log.info("데이터 플랫폼 전송 성공 및 오프셋 커밋 완료 - reservationId: {}, offset: {}", event.getReservationId(), offset);

        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패 - reservationId: {}, 재시도 예정", event.getReservationId(), e);
            throw e;  // 재시도를 위해 예외를 다시 던짐
        }
    }

    /**
     * DLT(Dead Letter Topic) 핸들러
     * 모든 재시도가 실패한 메시지를 처리
     */
    @DltHandler
    public void handleDlt(
            @Payload ReservationCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage
    ) {
        log.error("DLT 메시지 수신 - 모든 재시도 실패 | " + "topic: {}, partition: {}, offset: {}, reservationId: {}, userId: {}, error: {}",
                topic, partition, offset, event.getReservationId(), event.getUserId(), exceptionMessage);

        // TODO: 실패한 이벤트를 DB에 저장하여 수동 재처리
        // 예: failedEventRepository.save(FailedEvent.of(event, exceptionMessage));

        // TODO: 알람 발송 (Slack, Email 등)
        // 예: alertService.sendAlert("예약 데이터 전송 최종 실패", event);

        log.warn("DLT 처리 완료 - 수동 확인 필요 | reservationId: {}", event.getReservationId());
    }

    /**
     * 이벤트를 데이터 플랫폼 DTO로 변환
     */
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
