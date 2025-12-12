package com.gomdol.concert.common.presentation.exception;

import lombok.Getter;

import java.time.Duration;

/**
 * 분산 락 획득에 실패했을 때 발생하는 예외
 * Redis 락 등 외부 락 시스템 사용 시 락을 획득하지 못한 경우 사용
 */
@Getter
public class LockAcquisitionException extends RuntimeException {
    private final String lockKey;
    private final Duration waitTime;

    /**
     * 락 획득 실패 예외 생성
     *
     * @param lockKey  락 키 (예: "reservation:1234")
     * @param waitTime 락 획득 시도한 대기 시간
     */
    public LockAcquisitionException(String lockKey, Duration waitTime) {
        super(buildMessage(lockKey, waitTime));
        this.lockKey = lockKey;
        this.waitTime = waitTime;
    }

    /**
     * 원인 예외와 함께 락 획득 실패 예외 생성
     *
     * @param lockKey  락 키
     * @param waitTime 대기 시간
     * @param cause    원인 예외
     */
    public LockAcquisitionException(String lockKey, Duration waitTime, Throwable cause) {
        super(buildMessage(lockKey, waitTime), cause);
        this.lockKey = lockKey;
        this.waitTime = waitTime;
    }

    private static String buildMessage(String lockKey, Duration waitTime) {
        StringBuilder msg = new StringBuilder();
        msg.append("락 획득에 실패했습니다.");
        if (lockKey != null)
            msg.append(" [key=").append(lockKey).append("]");
        if (waitTime != null)
            msg.append(" [waitTime=").append(waitTime.toMillis()).append("ms]");

        return msg.toString();
    }
}
