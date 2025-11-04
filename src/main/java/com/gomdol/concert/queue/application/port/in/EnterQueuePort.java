package com.gomdol.concert.queue.application.port.in;

import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;

public interface EnterQueuePort {

    /**
     * 대기열 확인을 요청하면 대기열 정보를 반환한다.
     *
     * @param request 조회용 정보
     * @return QueueTokenResponse 대기열 정보를 반환
     */
    QueueTokenResponse enterQueue(QueueTokenRequest request);
    record QueueTokenRequest(Long targetId, String userId, String token) {}
}
