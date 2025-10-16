package com.gomdol.concert.queue.application.usecase;

import com.gomdol.concert.queue.application.port.in.IssueQueueTokenPort;
import com.gomdol.concert.queue.application.port.out.QueuePolicyProvider;
import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.application.port.out.TokenGenerator;
import com.gomdol.concert.queue.domain.model.QueueToken;
import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueQueueTokenUseCase implements IssueQueueTokenPort {

    private final QueueRepository queueRepository;
    private final TokenGenerator tokenGenerator;
    private final QueuePolicyProvider queuePolicyProvider;

    @Override
    public QueueTokenResponse issue(IssueCommand cmd) {
        // 토큰 조회
        log.info("creating token for {}", cmd);
        Optional<QueueToken> existed = queueRepository.findToken(cmd.targetId(), cmd.userId());
        if(existed.isPresent())
            return QueueTokenResponse.fromDomain(existed.get());

        // 멱등 발급
        QueueToken token = queueRepository.createToken(
                cmd.targetId(),
                cmd.userId(),
                tokenGenerator.newToken(),
                queuePolicyProvider.waitingTtlSeconds()
        );

        return QueueTokenResponse.fromDomain(token);
    }
}
