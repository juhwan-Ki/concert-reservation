package com.gomdol.concert.queue.application.usecase;

import com.gomdol.concert.queue.application.port.in.EnterQueuePort;
import com.gomdol.concert.queue.application.port.out.QueueRepository;
import com.gomdol.concert.queue.domain.model.QueueToken;
import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;
import com.gomdol.concert.show.application.port.out.ShowQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnterQueueUseCase implements EnterQueuePort {

    private final QueueRepository queueRepository;
    private final ShowQueryRepository showQueryRepository;

    @Override
    public QueueTokenResponse enterQueue(QueueTokenRequest request) {
        log.info("Enter queue token: {}", request);

        if(!showQueryRepository.existsById(request.targetId()))
            throw new IllegalArgumentException("존재하지 않는 공연입니다. 공연 ID : " + request.targetId());

        // 현재 만료 체크까지 DB에서 하고 넘어옴
        QueueToken queueToken = queueRepository.findByTargetIdAndToken(request.targetId(), request.token())
                .orElseThrow(() -> new IllegalArgumentException("대기열 토큰이 존재하지 않습니다."));

        if(queueToken.isExpired())
            throw new IllegalStateException("만료된 토큰입니다. 다시 발급받아주세요.");

        if(!queueToken.getUserId().equals(request.userId()))
            throw new IllegalArgumentException("토큰 소유자가 일치하지 않습니다.");

        return QueueTokenResponse.fromDomain(queueToken);
    }
}
