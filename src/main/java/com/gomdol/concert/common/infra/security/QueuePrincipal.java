package com.gomdol.concert.common.infra.security;

import java.security.Principal;

public class QueuePrincipal implements Principal {
    private final String userId;     // 원래 로그인된 유저 ID
    private final String queueToken; // 대기열 토큰 값
    private final boolean active;    // 대기열 통과 여부
    private final int position;      // 현재 순번

    public QueuePrincipal(String userId, String queueToken, boolean active, int position) {
        this.userId = userId;
        this.queueToken = queueToken;
        this.active = active;
        this.position = position;
    }

    @Override
    public String getName() {
        return "";
    }
}
