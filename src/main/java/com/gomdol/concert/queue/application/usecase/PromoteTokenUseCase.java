package com.gomdol.concert.queue.application.usecase;

import com.gomdol.concert.queue.application.port.in.PromoteTokenPort;
import com.gomdol.concert.queue.application.port.out.QueuePolicyProvider;
import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoteTokenUseCase implements PromoteTokenPort {

    private final QueueRepository queueRepository;
    private final QueuePolicyProvider queuePolicyProvider;

    @Override
    public int promote(Long targetId) {
        Instant now = Instant.now();
        int capacity = queuePolicyProvider.capacity();

        // 현재 입장중인 인원 수
        long enteredActive = queueRepository.countEnteredActiveWithLock(targetId, now); // status=ENTERED AND expires_at > now
        int roomSize = (int) Math.max(0, capacity - enteredActive);
        if (roomSize == 0)
            return 0;

        int promoted = 0;
        long ttlSeconds = queuePolicyProvider.enteredTtlSeconds();
        List<QueueToken> waitingTokens = queueRepository.findAndLockWaitingTokens(targetId, now, roomSize);
        for (QueueToken token : waitingTokens) {
            try {
                token.entered(ttlSeconds);
                queueRepository.save(token);
                promoted++;
            } catch (Exception e) {
                log.warn("예상치 못한 오류", e);
            }
        }
        return promoted;
    }
}
