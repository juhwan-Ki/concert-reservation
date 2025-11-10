package com.gomdol.concert.queue.application.port.in;

public interface PromoteTokenPort {
    /**
     * 토큰의 상태를 entered로 변경하여 예약이 가능하도록 변경한다
     *
     * @param targetId 공연 ID
     * @return int 대기열에서 몇명이 승격되었는지 리턴
     */
    int promote(Long targetId); // 스케줄러
}
