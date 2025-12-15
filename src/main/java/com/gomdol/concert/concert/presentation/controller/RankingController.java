package com.gomdol.concert.concert.presentation.controller;

import com.gomdol.concert.concert.application.port.in.GetFastSellingRankingPort;
import com.gomdol.concert.concert.domain.model.FastSellingConcert;
import com.gomdol.concert.concert.presentation.dto.FastSellingRankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.gomdol.concert.concert.presentation.dto.FastSellingRankingResponse.*;

/**
 * 랭킹 API Controller
 * - 콘서트 랭킹 조회
 */
@Slf4j
@Tag(name = "Ranking", description = "랭킹 API")
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final GetFastSellingRankingPort getFastSellingRanking;

    /**
     * 콘서트 랭킹 상위 N개 조회
     *
     * @param limit 조회할 개수 (기본 10개)
     * @return 콘서트 랭킹 목록
     */
    @Operation(summary = "콘서트 랭킹 조회", description = "판매 속도가 빠른 콘서트 상위 N개를 조회합니다.")
    @GetMapping("/fast-selling")
    public ResponseEntity<FastSellingRankingResponse> getFastSellingShows(
            @Parameter(description = "조회할 개수 (1-50)", example = "10")
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("콘서트 랭킹 조회 요청 - limit={}", limit);

        // limit 검증
        if (limit < 1 || limit > 50)
            throw new IllegalArgumentException("limit은 1~50 사이여야 합니다.");

        List<FastSellingConcert> rankings = getFastSellingRanking.getTopRanking(limit);
        FastSellingRankingResponse response = of(rankings);
        log.info("콘서트 랭킹 조회 완료 - count={}", rankings.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 콘서트의 랭킹 조회
     *
     * @param concertId 콘서트 ID
     * @return 콘서트의 랭킹 정보
     */
    @Operation(summary = "콘서트 랭킹 조회", description = "특정 콘서트의 랭킹 정보를 조회합니다.")
    @GetMapping("/fast-selling/{concertId}")
    public ResponseEntity<FastSellingConcertDto> getConcertRanking(
            @Parameter(description = "콘서트 ID", example = "1")
            @PathVariable Long concertId
    ) {
        log.info("콘서트 랭킹 조회 요청 - concertId={}", concertId);
        FastSellingConcert ranking = getFastSellingRanking.getConcertRanking(concertId)
                .orElseThrow(() -> new IllegalArgumentException("콘서트 랭킹 정보를 찾을 수 없습니다. concertId=" + concertId));

        FastSellingConcertDto response = FastSellingConcertDto.from(ranking);
        log.info("콘서트 랭킹 조회 완료 - concertId={}, rank={}", concertId, ranking.getRank());
        return ResponseEntity.ok(response);
    }
}
