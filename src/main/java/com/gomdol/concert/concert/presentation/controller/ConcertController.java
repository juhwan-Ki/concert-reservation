package com.gomdol.concert.concert.presentation.controller;

import com.gomdol.concert.common.presentation.dto.PageResponse;
import com.gomdol.concert.concert.application.usecase.ConcertQueryUseCase;
import com.gomdol.concert.concert.presentation.dto.ConcertDetailResponse;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import com.gomdol.concert.concert.presentation.dto.ShowResponseList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name="Concert", description = "콘서트 조회,단일 조회,공연 조회")
@RestController
@Validated
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertQueryUseCase concertQueryService;

    // TODO: 현재는 키워드로만 조회하도록 함, 추후 장르,인기순과 같은 검색 기능 추가 필요
    @Operation(summary = "콘서트 조회", description = "현재 진행중인 콘서트 리스트를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/")
    public ResponseEntity<PageResponse<ConcertResponse>> getConcertPage(
            @Schema(example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Schema(example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @Schema(example = "QWER") @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(concertQueryService.getConcertList(PageRequest.of(page, size), keyword));
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
        return ResponseEntity.ok(concertQueryService.getConcertById(concertId));
    }

    @Operation(summary = "공연 목록 조회", description = "특정 콘서트의 공연 일정을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ShowResponseList.class))),
            @ApiResponse(responseCode = "404", description = "없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{concertId}/shows")
    public ResponseEntity<ShowResponseList> geListShows(
            @Parameter(description = "콘서트 ID", example = "101") @PathVariable @Min(1) Long concertId
    ) {
        return ResponseEntity.ok(null);
    }
}
