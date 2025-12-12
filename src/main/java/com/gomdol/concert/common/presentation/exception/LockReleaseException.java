package com.gomdol.concert.common.presentation.exception;

import lombok.Getter;

/**
 * 분산 락 해제에 실패했을 때 발생하는 예외
 * 락을 정상적으로 해제하지 못한 경우 (예: 락이 이미 만료됨, 소유자가 아님)
 */
@Getter
public class LockReleaseException extends RuntimeException {
    private final String lockKey;

    /**
     * 원인 예외와 함께 락 해제 실패 예외 생성
     *
     * @param lockKey 락 키
     * @param message 상세 메시지
     * @param cause   원인 예외
     */
    public LockReleaseException(String lockKey, String message, Throwable cause) {
        super(buildMessage(lockKey, message), cause);
        this.lockKey = lockKey;
    }

    private static String buildMessage(String lockKey, String message) {
        StringBuilder msg = new StringBuilder();
        msg.append("락 해제에 실패했습니다.");
        if (lockKey != null) {
            msg.append(" [key=").append(lockKey).append("]");
        }
        if (message != null) {
            msg.append(" ").append(message);
        }
        return msg.toString();
    }
}
