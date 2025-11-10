package com.gomdol.concert.queue.application.port.in;

import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;

public interface IssueQueueTokenPort {
    /**
     * 토큰 발급을 요청하면 토큰 정보를 반환한다.
     *
     * @param cmd 토큰 발급에 사용되는 파라미터
     * @return QueueTokenResponse 토큰 정보 반환
     */
    QueueTokenResponse issue(IssueCommand cmd);
    record IssueCommand(String userId, Long targetId, String requestId) {}
}
