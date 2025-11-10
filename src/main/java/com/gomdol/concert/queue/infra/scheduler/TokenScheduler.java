package com.gomdol.concert.queue.infra.scheduler;

import com.gomdol.concert.queue.application.port.in.PromoteTokenPort;
import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.domain.model.QueueToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenScheduler {

    private final PromoteTokenPort promoteTokenPort;
    private final QueueRepository queueRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void updateTokens() {
        Instant now = Instant.now();
        List<Long> activeTargets = queueRepository.findActiveTargetIds(now);

        for (Long targetId : activeTargets) {
            try {
                int promoted = promoteTokenPort.promote(targetId);
                if (promoted > 0)
                    log.info("target {} → {}명 승급 완료", targetId, promoted);
            } catch (Exception e) {
                log.error("승급 실패 target={}", targetId, e);
            }
        }
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expireTokens() {
        Instant now = Instant.now();
        List<QueueToken> expireTokens = queueRepository.findAllExpiredAndOffsetLimit(now,50);

        for (QueueToken expireToken : expireTokens) {
            expireToken.expired();
            queueRepository.save(expireToken);
        }
    }
}
