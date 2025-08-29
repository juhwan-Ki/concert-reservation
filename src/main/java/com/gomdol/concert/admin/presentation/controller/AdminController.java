package com.gomdol.concert.admin.presentation.controller;

import com.gomdol.concert.admin.presentation.dto.ConcertCreateRequest;
import com.gomdol.concert.admin.presentation.dto.ConcertUpdateRequest;
import com.gomdol.concert.common.exception.ApiException;
import com.gomdol.concert.concert.presentation.dto.ConcertResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/v1/admins")
public class AdminController {

    /**
     * TODO: 현재는 콘서트와 공연이 1:1로 매칭되게 함 -> 추후 1:N으로 매핑해서 여러개의 공연을 처리하도록 변경 필요
     */
    @Operation(summary = "콘서트 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성됨",
                    content = @Content(schema = @Schema(implementation = ConcertResponse.class))),
            @ApiResponse(responseCode = "400", description = "검증 실패",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/concerts")
    public ResponseEntity<ConcertResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal token,
            @Valid @RequestBody ConcertCreateRequest request
    )
    {
        return ResponseEntity.created(null).body(null);
    }

    @Operation(summary = "콘서트 수정(patch)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ConcertResponse.class))),
            @ApiResponse(responseCode = "400", description = "검증 실패",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/concerts/{concertId}")
    public ResponseEntity<ConcertResponse> update(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal token,
            @Parameter(description = "콘서트 ID", example = "101") @PathVariable Long concertId,
            @Valid @RequestBody ConcertUpdateRequest request
    ) {
        return ResponseEntity.ok().body(null);
    }

    @Operation(summary = "콘서트 삭제(소프트)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제됨"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/concerts/{concertId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal token,
            @Parameter(description = "콘서트 ID", example = "101") @PathVariable Long concertId
    ) {
    }

    @Operation(summary = "공개 전환")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ConcertResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/concerts/{concertId}/publish")
    public ResponseEntity<ConcertResponse> publish(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal token,
            @Parameter(description = "콘서트 ID", example = "101") @PathVariable Long concertId
    ) {
        return ResponseEntity.ok().body(null);
    }

    @Operation(summary = "비공개 전환")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ConcertResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/concerts/{concertId}/unpublish")
    public ResponseEntity<ConcertResponse> unpublish(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal token,
            @Parameter(description = "콘서트 ID", example = "101") @PathVariable Long concertId
    ) {
        return ResponseEntity.ok().body(null);
    }
}
