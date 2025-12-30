package me.paulbaur.ict.coordination.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for managing distributed locks using Redis.
 * Enables coordination between multiple application instances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * Attempt to acquire a distributed lock.
     *
     * @param lockKey the lock key
     * @param waitTime maximum time to wait for the lock
     * @param leaseTime time after which the lock is automatically released
     * @param timeUnit time unit for waitTime and leaseTime
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (acquired) {
                log.debug("Acquired distributed lock", kv("lockKey", lockKey));
            } else {
                log.debug("Failed to acquire distributed lock", kv("lockKey", lockKey));
            }

            return acquired;
        } catch (InterruptedException e) {
            log.warn("Interrupted while trying to acquire lock",
                    kv("lockKey", lockKey),
                    e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Release a distributed lock.
     *
     * @param lockKey the lock key
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Released distributed lock", kv("lockKey", lockKey));
        } else {
            log.warn("Attempted to unlock a lock not held by current thread",
                    kv("lockKey", lockKey));
        }
    }

    /**
     * Check if a lock is currently held.
     *
     * @param lockKey the lock key
     * @return true if the lock is held by any thread
     */
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * Execute an action with a distributed lock.
     *
     * @param lockKey the lock key
     * @param waitTime maximum time to wait for the lock
     * @param leaseTime time after which the lock is automatically released
     * @param timeUnit time unit for waitTime and leaseTime
     * @param action the action to execute while holding the lock
     * @return true if the action was executed, false if lock could not be acquired
     */
    public boolean executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable action) {
        if (tryLock(lockKey, waitTime, leaseTime, timeUnit)) {
            try {
                action.run();
                return true;
            } finally {
                unlock(lockKey);
            }
        }
        return false;
    }
}
