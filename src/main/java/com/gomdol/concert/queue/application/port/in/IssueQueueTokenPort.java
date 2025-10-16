package com.gomdol.concert.queue.application.port.in;

import com.gomdol.concert.queue.presentation.dto.QueueTokenResponse;

public interface IssueQueueTokenPort {
    QueueTokenResponse issue(IssueCommand cmd);
    record IssueCommand(String userId, Long targetId, String requestId) {}
}
