package com.gomdol.concert.payment.domain.event;

import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트
 * - 결제가 성공적으로 완료되었을 때 발행
 * - 랭킹 업데이트를 위한 콘서트 정보 포함
 */
public record PaymentCompletedEvent(
        Long paymentId,
        Long reservationId,
        Long showId,
        Long concertId,
        String userId,
        int seatCount,
        // 랭킹을 위한 콘서트 정보
        String concertTitle,
        int totalSeats,
        int reservedSeats,
        LocalDateTime completedAt
) {
    public static PaymentCompletedEvent of(
            Long paymentId,
            Long reservationId,
            Long showId,
            Long concertId,
            String userId,
            int seatCount,
            String concertTitle,
            int totalSeats,
            int reservedSeats
    ) {
        return new PaymentCompletedEvent(
                paymentId,
                reservationId,
                showId,
                concertId,
                userId,
                seatCount,
                concertTitle,
                totalSeats,
                reservedSeats,
                LocalDateTime.now()
        );
    }
}
