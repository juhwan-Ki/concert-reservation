package com.gomdol.concert.queue.application.port.out;

import com.gomdol.concert.queue.domain.model.QueueToken;

import java.util.List;
import java.util.Optional;

public interface QueueRepository {
    // 발급
    QueueToken createToken(Long targetId, String userId, String token, long waitingTtlSeconds);

    // 조회
    Optional<QueueToken> findToken(Long targetId, String userId);
    Long rankInWaiting(Long targetId, String token); // 0-based, 없으면 null
    Long waitingSize(Long targetId);
    boolean isActive(Long targetId, String token);
    // 승격
    List<String> pollNextWaiting(Long targetId, int n); // 앞에서 n개 pop
    void activate(Long targetId, List<String> tokens, long activeTtlSec);

    // 만료
    void expireIfNeeded(Long targetId, String token);
}
