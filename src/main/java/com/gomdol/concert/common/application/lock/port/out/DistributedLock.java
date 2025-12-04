package com.gomdol.concert.common.application.lock.port.out;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 분산 락 Port (Output Port)
 * - Redis 기반 분산 락을 추상화
 * - Redisson, Lettuce 등 다양한 구현체로 교체 가능
 */
public interface DistributedLock {

    /**
     * 락을 획득하고 작업 실행
     *
     * @param lockKey 락 키 (예: "reservation:seat:1")
     * @param waitTime 락 획득 대기 시간
     * @param leaseTime 락 자동 해제 시간
     * @param timeUnit 시간 단위
     * @param supplier 락 획득 후 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 실행 결과
     * @throws IllegalStateException 락 획득 실패 시
     */
    <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier);

    /**
     * 락을 획득하고 작업 실행 (void 반환)
     *
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간
     * @param leaseTime 락 자동 해제 시간
     * @param timeUnit 시간 단위
     * @param runnable 락 획득 후 실행할 작업
     * @throws IllegalStateException 락 획득 실패 시
     */
    void executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable runnable);
}
