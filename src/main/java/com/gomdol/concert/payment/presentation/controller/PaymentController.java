package com.gomdol.concert.payment.presentation.controller;

import com.gomdol.concert.common.presentation.exception.ApiException;
import com.gomdol.concert.common.infra.security.QueuePrincipal;
import com.gomdol.concert.payment.application.facade.PaymentFacade;
import com.gomdol.concert.payment.application.port.in.SavePaymentPort.PaymentCommand;
import com.gomdol.concert.payment.presentation.dto.PaymentRequest;
import com.gomdol.concert.payment.presentation.dto.PaymentResponse;
import com.gomdol.concert.payment.presentation.dto.RefundResponse;
import com.sun.security.auth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @Operation(summary = "결제",
            description = "예약 ID와 금액으로 즉시 결제를 시도한다. (멱등성 지원: Idempotency-Key)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 성공",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "검증 실패",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "404", description = "예약 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "상태 충돌(이미 확정/만료/취소/금액불일치)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/")
    public ResponseEntity<PaymentResponse> pay(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @Valid @RequestBody PaymentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user,
            @Parameter(hidden = true) @RequestAttribute("queuePrincipal") QueuePrincipal queue // 시큐리티에서 처리 예정
    ) {
        String userId = user != null ? user.getName() : "test-user"; // TODO: 실제 인증 구현 시 수정
        String requestId = idemKey != null ? idemKey : java.util.UUID.randomUUID().toString();
        PaymentCommand command = new PaymentCommand(request.reservationId(), userId, requestId, request.amount());

        // Facade를 통해 멱등성 보장 + 분산 락 + 동기 결제 처리
        PaymentResponse response = paymentFacade.processPayment(command);
        return ResponseEntity.ok(response);
    }

    // TODO: 현재 부분 환불은 하지 않고 전체 환불만 가능하도록 함
    @Operation(summary = "예약 취소(환불 요청)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "환불 요청 성공",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "404", description = "예약 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "환불 불가 상태 (기간 만료 등)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<RefundResponse> cancelReservation(
            @PathVariable Long reservationId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(null);
    }
}
