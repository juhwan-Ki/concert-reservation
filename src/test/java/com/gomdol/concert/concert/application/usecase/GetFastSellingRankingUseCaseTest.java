package com.gomdol.concert.concert.application.usecase;

import com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository;
import com.gomdol.concert.concert.domain.model.FastSellingConcert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.gomdol.concert.concert.application.port.out.FastSellingRankingRepository.ConcertStats;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * GetFastSellingRankingUseCase 테스트
 */
@ExtendWith(MockitoExtension.class)
class GetFastSellingRankingUseCaseTest {

    @Mock
    private FastSellingRankingRepository rankingRepository;

    @InjectMocks
    private GetFastSellingRankingUseCase useCase;

    @Test
    @DisplayName("상위 N개 콘서트 랭킹을 조회한다")
    void getTopRanking() {
        // given
        List<Long> topConcertIds = List.of(1L, 2L, 3L);
        Map<Long, ConcertStats> statsMap = Map.of(
                1L, new ConcertStats("아이유 콘서트", 1000, 800, 50),
                2L, new ConcertStats("QWER 콘서트", 2000, 1500, 80),
                3L, new ConcertStats("데이식스 콘서트", 1500, 900, 60));

        when(rankingRepository.getTopConcertIds(3)).thenReturn(topConcertIds);
        when(rankingRepository.getConcertStats(topConcertIds)).thenReturn(statsMap);

        // when
        List<FastSellingConcert> result = useCase.getTopRanking(3);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(1).getRank()).isEqualTo(2);
        assertThat(result.get(2).getRank()).isEqualTo(3);

        assertThat(result.get(0).getConcertId()).isEqualTo(1L);
        assertThat(result.get(0).getConcertTitle()).isEqualTo("아이유 콘서트");
    }

    @Test
    @DisplayName("랭킹 데이터가 없으면 빈 리스트를 반환한다")
    void getTopRanking_EmptyWhenNoData() {
        // given
        when(rankingRepository.getTopConcertIds(10)).thenReturn(List.of());

        // when
        List<FastSellingConcert> result = useCase.getTopRanking(10);

        // then
        assertThat(result).isEmpty();
        verify(rankingRepository, never()).getConcertStats(anyList());
    }

    @Test
    @DisplayName("통계 정보가 없는 콘서트는 랭킹에서 제외된다")
    void getTopRanking_FilterOutConcertsWithoutStats() {
        // given
        List<Long> topConcertIds = List.of(1L, 2L, 3L);
        // 2번은 존재 하지 않음
        Map<Long, ConcertStats> statsMap = Map.of(
                1L, new ConcertStats("아이유 콘서트", 1000, 800, 50),
                3L, new ConcertStats("블랙핑크 콘서트", 1500, 900, 60));

        when(rankingRepository.getTopConcertIds(3)).thenReturn(topConcertIds);
        when(rankingRepository.getConcertStats(topConcertIds)).thenReturn(statsMap);

        // when
        List<FastSellingConcert> result = useCase.getTopRanking(3);

        // then
        assertThat(result).hasSize(2); // 3개가 아닌 2개만 반환
        assertThat(result.stream().map(FastSellingConcert::getConcertId)).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("순위는 1부터 시작한다")
    void getTopRanking_RankStartsFromOne() {
        // given
        List<Long> topConcertIds = List.of(5L);
        Map<Long, ConcertStats> statsMap = Map.of(5L, new ConcertStats("콘서트", 1000, 500, 20));

        when(rankingRepository.getTopConcertIds(1)).thenReturn(topConcertIds);
        when(rankingRepository.getConcertStats(topConcertIds)).thenReturn(statsMap);

        // when
        List<FastSellingConcert> result = useCase.getTopRanking(1);

        // then
        assertThat(result.get(0).getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("판매율과 판매속도가 올바르게 계산된다")
    void getTopRanking_CalculatesSalesRateAndSpeed() {
        // given
        List<Long> topConcertIds = List.of(1L);
        Map<Long, ConcertStats> statsMap = Map.of(1L, new ConcertStats("콘서트", 1000, 500, 30));

        when(rankingRepository.getTopConcertIds(1)).thenReturn(topConcertIds);
        when(rankingRepository.getConcertStats(topConcertIds)).thenReturn(statsMap);

        // when
        List<FastSellingConcert> result = useCase.getTopRanking(1);

        // then
        FastSellingConcert show = result.get(0);
        assertThat(show.getSalesRate()).isEqualTo(50.0); // 500/1000 * 100
        assertThat(show.getSalesSpeed()).isEqualTo(30.0);
        assertThat(show.getRankingScore()).isEqualTo(350.0); // 50 + 30*10
    }

    @Test
    @DisplayName("특정 콘서트의 랭킹 정보를 조회한다")
    void getConcertRanking() {
        // given
        Long concertId = 5L;
        Map<Long, ConcertStats> statsMap = Map.of(5L, new ConcertStats("아이유 콘서트", 1000, 700, 40));

        when(rankingRepository.getConcertScore(concertId)).thenReturn(Optional.of(470.0));
        when(rankingRepository.getConcertRank(concertId)).thenReturn(Optional.of(2L)); // 0-based
        when(rankingRepository.getConcertStats(List.of(concertId))).thenReturn(statsMap);

        // when
        Optional<FastSellingConcert> result = useCase.getConcertRanking(concertId);

        // then
        assertThat(result).isPresent();
        FastSellingConcert show = result.get();
        assertThat(show.getConcertId()).isEqualTo(5L);
        assertThat(show.getRank()).isEqualTo(3); // 0-based rank 2 → 1-based rank 3
        assertThat(show.getConcertTitle()).isEqualTo("아이유 콘서트");
    }

    @Test
    @DisplayName("점수가 없는 콘서트는 랭킹 조회 시 Empty를 반환한다")
    void getConcertRanking_EmptyWhenNoScore() {
        // given
        Long concertId = 5L;
        when(rankingRepository.getConcertScore(concertId)).thenReturn(Optional.empty());

        // when
        Optional<FastSellingConcert> result = useCase.getConcertRanking(concertId);

        // then
        assertThat(result).isEmpty();
        verify(rankingRepository, never()).getConcertRank(anyLong());
        verify(rankingRepository, never()).getConcertStats(anyList());
    }

    @Test
    @DisplayName("순위가 없는 콘서트는 랭킹 조회 시 Empty를 반환한다")
    void getConcertRanking_EmptyWhenNoRank() {
        // given
        Long concertId = 5L;
        when(rankingRepository.getConcertScore(concertId)).thenReturn(Optional.of(100.0));
        when(rankingRepository.getConcertRank(concertId)).thenReturn(Optional.empty());

        // when
        Optional<FastSellingConcert> result = useCase.getConcertRanking(concertId);

        // then
        assertThat(result).isEmpty();
        verify(rankingRepository, never()).getConcertStats(anyList());
    }

    @Test
    @DisplayName("통계가 없는 콘서트는 랭킹 조회 시 Empty를 반환한다")
    void getConcertRanking_EmptyWhenNoStats() {
        // given
        Long concertId = 5L;
        when(rankingRepository.getConcertScore(concertId)).thenReturn(Optional.of(100.0));
        when(rankingRepository.getConcertRank(concertId)).thenReturn(Optional.of(0L));
        when(rankingRepository.getConcertStats(List.of(concertId))).thenReturn(Map.of());

        // when
        Optional<FastSellingConcert> result = useCase.getConcertRanking(concertId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("1위 콘서트의 랭킹을 조회한다")
    void getConcertRanking_FirstPlace() {
        // given
        Long concertId = 1L;
        Map<Long, ConcertStats> statsMap = Map.of(1L, new ConcertStats("QWER 콘서트", 2000, 1900, 100));

        when(rankingRepository.getConcertScore(concertId)).thenReturn(Optional.of(1095.0));
        when(rankingRepository.getConcertRank(concertId)).thenReturn(Optional.of(0L));
        when(rankingRepository.getConcertStats(List.of(concertId))).thenReturn(statsMap);

        // when
        Optional<FastSellingConcert> result = useCase.getConcertRanking(concertId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getRank()).isEqualTo(1);
    }
}
