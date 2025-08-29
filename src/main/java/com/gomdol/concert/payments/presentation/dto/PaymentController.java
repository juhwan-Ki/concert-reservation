package com.gomdol.concert.payments.presentation.dto;

import com.gomdol.concert.common.exception.ApiException;
import com.gomdol.concert.payments.presentation.controller.ChargeResponse;
import com.sun.security.auth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Operation(summary = "결제",
            description = "예약 ID와 금액으로 즉시 결제를 시도한다. (멱등성 지원: Idempotency-Key)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 성공",
                    content = @Content(schema = @Schema(implementation = ChargeResponse.class))),
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
    public ResponseEntity<ChargeResponse> pay(
            @RequestHeader(value = "Idempotencyxx-Key", required = false) String idemKey,
            @Valid @RequestBody ChargeRequest req,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(null);
    }
}
