package com.gomdol.concert.queue.infra.scheduler;

import com.gomdol.concert.queue.application.port.in.PromoteTokenPort;
import com.gomdol.concert.queue.application.port.out.QueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Redis 기반 대기열 토큰 스케줄러
 * - WAITING → ENTERED 승급 처리
 * - 만료 처리는 countEnteredActiveWithLock()에서 자동 정리
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.repository", havingValue = "redis", matchIfMissing = false)
public class RedisTokenScheduler {

    private final PromoteTokenPort promoteTokenPort;
    private final QueueRepository queueRepository;

    /**
     * 매분마다 WAITING 토큰을 ENTERED로 승급
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void promoteWaitingTokens() {
        Instant now = Instant.now();
        List<Long> activeTargets = queueRepository.findActiveTargetIds(now);

        if (activeTargets.isEmpty()) {
            log.debug("승급할 대상 없음");
            return;
        }

        int totalPromoted = 0;
        for (Long targetId : activeTargets) {
            try {
                int promoted = promoteTokenPort.promote(targetId);
                if (promoted > 0) {
                    log.info("target {} → {}명 승급 완료", targetId, promoted);
                    totalPromoted += promoted;
                }
            } catch (Exception e) {
                log.error("승급 실패 target={}", targetId, e);
            }
        }

        if (totalPromoted > 0)
            log.info("총 {}명 승급 완료", totalPromoted);
    }
}
