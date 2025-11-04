package com.gomdol.concert.queue.domain.model;

public enum QueueStatus {
    WAITING("대기 중"), ENTERED("입장 완료"), EXPIRED("만료");

    private final String desc;

    QueueStatus(String desc) {
        this.desc = desc;
    }
}
