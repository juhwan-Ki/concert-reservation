package com.gomdol.concert.reservation.presentation.controller;

import com.gomdol.concert.common.exception.ApiException;
import com.gomdol.concert.common.security.QueuePrincipal;
import com.gomdol.concert.concert.presentation.dto.ShowResponseList;
import com.gomdol.concert.reservation.presentation.dto.*;
import com.sun.security.auth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reservation", description = "예약 가능 날짜/좌석 조회, 예약/취소")
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    @Operation(summary = "예약 가능 날짜 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ShowResponseList.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{concertId}")
    public ResponseEntity<ShowResponseList> getReservations(
            @Parameter(description = "콘서트 ID", example = "101")
            @PathVariable Long concertId
    ) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "예약 가능 좌석 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = SeatAvailabilityResponseList.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{concertId}/{showId}")
    public ResponseEntity<SeatAvailabilityResponseList> getAvailableSeats(
            @Parameter(description = "콘서트 ID", example = "101")
            @PathVariable Long concertId,
            @Parameter(description = "회차 ID", example = "202")
            @PathVariable Long showId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user,
            @Parameter(hidden = true) @RequestAttribute("queuePrincipal") QueuePrincipal queue // 시큐리티에서 처리 예정
    ) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "좌석 예약",
            description = "선택한 좌석을 임시 점유하고 예약을 생성한다. 결제 마감 시간(expiresAt)을 함께 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성됨",
                    content = @Content(schema = @Schema(implementation = ReservationCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "검증 실패",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "404", description = "회차/좌석 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "좌석 점유 충돌(이미 예약/홀드됨)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            })
    @PostMapping("/")
    public ResponseEntity<ReservationCreateResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user,
            @Parameter(hidden = true) @RequestAttribute("queuePrincipal") QueuePrincipal queue // 시큐리티에서 처리 예정
    ) {
        // return ResponseEntity.created(URI.create("/api/v1/reservations/" + id)).body(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @Operation(summary = "내 예약 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservationResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "예약 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ReservationDetail.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "404", description = "예약 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationDetail> getReservation(
            @PathVariable Long reservationId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(null);
    }
}
