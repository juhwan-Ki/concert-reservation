package com.gomdol.concert.point.presentation.controller;

import com.gomdol.concert.common.exception.ApiException;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.presentation.dto.PointHistoryPage;
import com.gomdol.concert.point.presentation.dto.PointRequest;
import com.gomdol.concert.point.presentation.dto.PointResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Tag(name = "Point", description = "포인트 조회/포인트 충전/포인트 내역 조회")
@RestController
@RequestMapping("/api/v1/users/me/points")
public class PointController {

    @Operation(summary = "내 포인트 조회", description = "현재 로그인한 사용자의 포인트 잔액을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = PointResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/")
    public ResponseEntity<PointResponse> getMyPoint(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(new PointResponse(10000L, LocalDateTime.now()));
    }

    @Operation(summary = "내 포인트 충전", description = "현재 로그인한 사용자의 포인트를 충전한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = PointResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "422", description = "도메인 규칙 위반(예: 최대/최소 충전 한도 초과)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/charges")
    public ResponseEntity<PointResponse> chargePoint(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody PointRequest request) {
        return ResponseEntity.ok(new PointResponse(10000L ,LocalDateTime.now()));
    }

    @Operation(summary = "내 포인트 내역 조회", description = "현재 로그인한 사용자의 포인트 내역을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = PointHistoryPage.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/histories")
    public ResponseEntity<PointHistoryPage> getMyPointHistories(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) UseType type,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to
            ) {
        return ResponseEntity.ok(null);
    }
}
