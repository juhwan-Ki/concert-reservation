package com.gomdol.concert.common.infra.lock;

import com.gomdol.concert.common.application.lock.port.out.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

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

            if (!acquired) {
                log.warn("락 획득 실패 - lockKey: {}, waitTime: {}ms", lockKey, timeUnit.toMillis(waitTime));
                throw new IllegalStateException("락을 획득할 수 없습니다. 다시 시도해주세요.");
            }

            log.debug("락 획득 성공 - lockKey: {}", lockKey);
            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생 - lockKey: {}", lockKey, e);
            throw new IllegalStateException("락 획득 중 오류가 발생했습니다.", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("락 해제 완료 - lockKey: {}", lockKey);
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
