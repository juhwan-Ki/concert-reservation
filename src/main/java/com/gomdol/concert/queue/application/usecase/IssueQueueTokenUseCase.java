package com.gomdol.concert.queue.application.usecase;

import com.gomdol.concert.queue.application.port.in.IssueQueueTokenPort;
import com.gomdol.concert.queue.application.port.out.QueuePolicyProvider;
import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.application.port.out.TokenGenerator;
import com.gomdol.concert.queue.domain.model.QueueStatus;
import com.gomdol.concert.queue.domain.model.QueueToken;
import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueQueueTokenUseCase implements IssueQueueTokenPort {

    private final QueueRepository queueRepository;
    private final TokenGenerator tokenGenerator;
    private final QueuePolicyProvider queuePolicyProvider;

    @Override
    @Transactional
    public QueueTokenResponse issue(IssueCommand cmd) {
        log.info("creating token for {}", cmd);
        // 토큰 조회
        Optional<QueueToken> existed = queueRepository.findByTargetIdAndUserId(cmd.targetId(), cmd.userId());
        if(existed.isPresent())
            return QueueTokenResponse.fromDomain(existed.get());

        // 현재 입장한 사용자 수 및 대기 중인 사용자 확인
        long enteredCount = queueRepository.countEnteredActive(cmd.targetId(), Instant.now());
        int capacity = queuePolicyProvider.capacity();
        boolean hasWaitingUsers = queueRepository.isWaiting(cmd.targetId());

        // capacity가 꽉 찼거나 대기 중인 사용자가 있으면 WAITING
        boolean shouldWait = enteredCount >= capacity || hasWaitingUsers;
        QueueStatus status = shouldWait ? QueueStatus.WAITING : QueueStatus.ENTERED;
        long ttlSeconds = shouldWait ? queuePolicyProvider.waitingTtlSeconds() : queuePolicyProvider.enteredTtlSeconds();

        QueueToken token = queueRepository.issueToken(cmd.targetId(), cmd.userId(), tokenGenerator.newToken(), status, ttlSeconds);

        return QueueTokenResponse.fromDomain(token);
    }
}
