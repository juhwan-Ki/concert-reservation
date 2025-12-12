package com.gomdol.concert.point.presentation.controller;

import com.gomdol.concert.common.presentation.dto.PageResponse;
import com.gomdol.concert.common.presentation.exception.ApiException;
import com.gomdol.concert.point.application.port.in.GetPointBalancePort;
import com.gomdol.concert.point.application.usecase.SavePointUseCase;
import com.gomdol.concert.point.domain.model.UseType;
import com.gomdol.concert.point.presentation.dto.PointHistoryResponse;
import com.gomdol.concert.point.presentation.dto.PointRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.time.OffsetDateTime;

import static com.gomdol.concert.point.application.port.in.GetPointBalancePort.*;
import static com.gomdol.concert.point.application.port.in.SavePointPort.*;

@Tag(name = "Point", description = "포인트 조회/포인트 충전/포인트 내역 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/points")
public class PointController {

    private final GetPointBalancePort getPointBalancePort;
    private final SavePointUseCase savePointUseCase;

    @Operation(summary = "내 포인트 조회", description = "현재 로그인한 사용자의 포인트 잔액을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = PointSearchResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/")
    public ResponseEntity<PointSearchResponse> getMyPoint(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me) {
        // TODO: 시큐리티 구현 필요
        return ResponseEntity.ok(getPointBalancePort.getPoint(me.getName()));
    }

    @Operation(summary = "내 포인트 충전", description = "현재 로그인한 사용자의 포인트를 충전한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = PointSaveResponse.class))),
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
    public ResponseEntity<PointSaveResponse> chargePoint(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me,
                                                         @Valid @RequestBody PointRequest request) {
        return ResponseEntity.ok(savePointUseCase.savePoint(request));
    }

    @Operation(summary = "내 포인트 내역 조회", description = "현재 로그인한 사용자의 포인트 내역을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/histories")
    public ResponseEntity<PageResponse<PointHistoryResponse>> getMyPointHistories(
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
