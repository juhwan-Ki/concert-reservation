package com.gomdol.concert.reservation.infra.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터 플랫폼 외부 API 클라이언트
 * 예약 완료 데이터를 외부 플랫폼으로 전송
 */
@Slf4j
@Component
public class DataPlatformClient {

    private final WebClient webClient;
    private final Duration timeout;

    public DataPlatformClient(
            @Value("${data-platform.base-url}") String baseUrl,
            @Value("${data-platform.timeout-seconds}") long timeoutSeconds,
            WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    /**
     * 예약 완료 데이터를 데이터 플랫폼으로 전송
     * 실패 시 재시도 (최대 3회, 1초 간격)
     */
    @Retryable(
            retryFor = {WebClientResponseException.class, Exception.class},
            maxAttemptsExpression = "${data-platform.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${data-platform.retry.backoff-millis}")
    )
    public void sendReservationData(ReservationDataDto data) {
        try {
            log.info("데이터 플랫폼으로 예약 정보 전송 시작 - reservationId: {}", data.reservationId());

            webClient.post()
                    .uri("/api/reservations")
                    .bodyValue(data)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(timeout)
                    .block();

            log.info("데이터 플랫폼으로 예약 정보 전송 완료 - reservationId: {}", data.reservationId());

        } catch (WebClientResponseException e) {
            log.error("데이터 플랫폼 API 호출 실패 - status: {}, body: {}, reservationId: {}",
                    e.getStatusCode(), e.getResponseBodyAsString(), data.reservationId(), e);
            throw e;
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 중 예외 발생 - reservationId: {}", data.reservationId(), e);
            throw e;
        }
    }

    /**
     * 데이터 플랫폼으로 전송할 DTO
     */
    public record ReservationDataDto(
            Long reservationId,
            String userId,
            String reservationCode,
            List<SeatData> seats,
            Long totalAmount,
            LocalDateTime confirmedAt,
            LocalDateTime sentAt
    ) {
        public record SeatData(
                Long seatId,
                String seatNumber,
                Long showId,
                Long price
        ) {}
    }
}
