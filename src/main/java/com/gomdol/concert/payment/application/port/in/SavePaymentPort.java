package com.gomdol.concert.payment.application.port.in;

import com.gomdol.concert.payment.presentation.dto.PaymentResponse;

public interface SavePaymentPort {
    PaymentResponse processPayment(PaymentCommand command);
    record PaymentCommand(Long reservationId, String userId, String requestId, long amount) {}
//    record PaymentResponse(Long paymentId, Long reservationId, String paymentCode, String requestId,
//                           String status, Long amount, LocalDateTime paidAt) {
//        public static PaymentResponse fromDomain(Payment payment) {
//            return new PaymentResponse(
//                    payment.getId(),
//                    payment.getReservationId(),
//                    payment.getPaymentCode(),
//                    payment.getRequestId(),
//                    payment.getStatus().toString(),
//                    payment.getAmount(),
//                    payment.getPaidAt()
//            );
//        }
//    }
}
