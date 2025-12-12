package com.gomdol.concert.common.infra.lock;

import com.gomdol.concert.common.application.lock.port.out.DistributedLock;
import com.gomdol.concert.common.presentation.exception.LockAcquisitionException;
import com.gomdol.concert.common.presentation.exception.LockReleaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 기반 분산 락 구현체
 * - Redis를 사용한 분산 락 제공
 * - Pub/Sub 메커니즘으로 효율적인 락 대기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonDistributedLock implements DistributedLock {

    private final RedissonClient redissonClient;

    @Override
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired)
                throw new LockAcquisitionException(lockKey, Duration.ofMillis(timeUnit.toMillis(waitTime)));

            log.debug("락 획득 성공 - lockKey: {}", lockKey);
            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생 - lockKey: {}", lockKey, e);
            throw new LockAcquisitionException(lockKey, Duration.ofMillis(timeUnit.toMillis(waitTime)), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.debug("락 해제 완료 - lockKey: {}", lockKey);
                } catch (Exception e) {
                    log.error("락 해제 실패 - lockKey: {}", lockKey, e);
                    throw new LockReleaseException(lockKey, "락 해제 중 오류 발생", e);
                }
            }
        }
    }

    @Override
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }
}
