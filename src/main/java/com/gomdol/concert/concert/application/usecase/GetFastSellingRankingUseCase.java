package com.gomdol.concert.concert.application.usecase;

import com.gomdol.concert.concert.application.port.in.GetFastSellingRankingPort;
import com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository;
import com.gomdol.concert.concert.domain.model.FastSellingConcert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository.*;

/**
 * 랭킹 조회 UseCase
 * - 콘서트 단위로 랭킹 표시
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetFastSellingRankingUseCase implements GetFastSellingRankingPort {

    private final FastSellingRankingRepository rankingRepository;

    @Override
    public List<FastSellingConcert> getTopRanking(int topNum) {
        log.debug("상위 {} 콘서트 조회", topNum);

        // Redis에서 상위 N개 concertId 조회
        List<Long> topConcertIds = rankingRepository.getTopConcertIds(topNum);
        if (topConcertIds.isEmpty()) {
            log.debug("랭킹 데이터 없음");
            return List.of();
        }

        // Redis에서 통계 조회
        Map<Long, ConcertStats> statsMap = rankingRepository.getConcertStats(topConcertIds);

        // FastSellingShow 생성 (순위 포함)
        List<FastSellingConcert> rankings = IntStream.range(0, topConcertIds.size())
                .mapToObj(i -> {
                    Long concertId = topConcertIds.get(i);
                    ConcertStats stats = statsMap.get(concertId);

                    if (stats == null) {
                        log.warn("통계 정보 없음 - concertId={}", concertId);
                        return null;
                    }

                    FastSellingConcert ranking = FastSellingConcert.of(concertId, stats.concertTitle(), stats.totalSeats(), stats.reservedSeats(), stats.lastHourSales());
                    return ranking.withRank(i + 1);  // 순위 (1부터 시작)
                })
                .filter(Objects::nonNull)
                .toList();

        log.debug("콘서트 {} 개 조회 완료", rankings.size());
        return rankings;
    }

    @Override
    public Optional<FastSellingConcert> getConcertRanking(Long concertId) {
        log.debug("콘서트 랭킹 조회- concertId={}", concertId);

        // 점수 조회
        Optional<Double> scoreOpt = rankingRepository.getConcertScore(concertId);
        if (scoreOpt.isEmpty()) {
            log.debug("랭킹 점수 없음 - concertId={}", concertId);
            return Optional.empty();
        }

        // 순위 조회
        Optional<Long> rankOpt = rankingRepository.getConcertRank(concertId);
        if (rankOpt.isEmpty()) {
            log.debug("랭킹 순위 없음 - concertId={}", concertId);
            return Optional.empty();
        }

        // 통계 조회
        Map<Long, ConcertStats> statsMap = rankingRepository.getConcertStats(List.of(concertId));
        ConcertStats stats = statsMap.get(concertId);

        if (stats == null) {
            log.debug("통계 정보 없음 - concertId={}", concertId);
            return Optional.empty();
        }

        FastSellingConcert ranking = FastSellingConcert.of(concertId, stats.concertTitle(), stats.totalSeats(), stats.reservedSeats(), stats.lastHourSales());
        FastSellingConcert result = ranking.withRank((int) (rankOpt.get() + 1));  // 순위 (1부터 시작)
        log.debug("콘서트 랭킹 조회 완료 - concertId={}, rank={}", concertId, result.getRank());
        return Optional.of(result);
    }
}
