package com.gomdol.concert.queue.presentation.controller;

import com.gomdol.concert.common.exception.ApiException;
import com.gomdol.concert.common.security.QueuePrincipal;
import com.gomdol.concert.queue.presentation.dto.QueueStatusResponse;
import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;
import com.sun.security.auth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Queue", description = "대기열 토큰 발급/상태 확인")
@RestController
@RequestMapping("/api/v1/queues")
public class QueueController {

    @Operation(summary = "대기열 토큰 발급",
            description = "유저 토큰으로 대기열에 진입하고 토큰을 발급받는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = QueueTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "429", description = "대기열 만석",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/tokens")
    public ResponseEntity<QueueTokenResponse> issueToken(
            @Parameter(required = false) @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @Operation(summary = "대기열 상태 확인",
            description = "본인의 대기 순서, 예상 대기 시간, 입장 가능 여부를 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = QueueStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class)))
    })
    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> checkStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal QueuePrincipal principal) {
        return ResponseEntity.ok(null);
    }
}
