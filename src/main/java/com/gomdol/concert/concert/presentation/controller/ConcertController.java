package com.gomdol.concert.concert.presentation.controller;

import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertPage;
import com.gomdol.concert.concert.presentation.dto.ShowPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name="Concert", description = "콘서트 조회,단일 조회,공연 조회")
@RestController
@RequestMapping("/api/v1/concerts")
public class ConcertController {

    @Operation(summary = "콘서트 조회", description = "현재 진행중인 콘서트 리스트를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ConcertPage.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/")
    public ResponseEntity<ConcertPage> getConcertPage(
            @Schema(example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Schema(example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Schema(example = "QWER") @RequestParam(required = false) String keyword,
            @Schema(description = "시작날짜(이상)", example = "2025-08-01")
            @RequestParam(required = false) LocalDateTime from,
            @Schema(description = "종료날짜(미만)", example = "2025-08-31")
            @RequestParam(required = false) LocalDateTime to
    ) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "콘서트 상세 조회", description = "현재 진행중인 콘서트 상세 내역을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ConcertDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertDetailResponse> getConcertDetail(@PathVariable @Min(1) Long concertId) {
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "공연 목록 조회", description = "특정 콘서트의 공연 일정을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ShowPage.class))),
            @ApiResponse(responseCode = "404", description = "없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{concertId}/shows")
    public ResponseEntity<ShowPage> geListShows(
            @PathVariable @Min(1) Long concertId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(null);
    }
}
