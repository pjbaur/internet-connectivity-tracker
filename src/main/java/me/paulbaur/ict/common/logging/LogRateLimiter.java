package me.paulbaur.ict.common.logging;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal, dependency-free rate limiter for log events.
 *
 * <p>Tracks the last-log timestamp per key and permits a new log only if the
 * configured interval has elapsed. Intended to dampen noisy warnings without
 * hiding the first occurrence.</p>
 */
public class LogRateLimiter {

    private final Map<String, Long> lastLoggedByKey = new ConcurrentHashMap<>();
    private final long intervalMillis;
    private final Clock clock;

    public LogRateLimiter(Duration interval) {
        this(interval, Clock.systemUTC());
    }

    LogRateLimiter(Duration interval, Clock clock) {
        this.intervalMillis = interval.toMillis();
        this.clock = clock;
    }

    /**
     * Returns true if the caller should emit a log for the given key right now.
     */
    public boolean shouldLog(String key) {
        long now = clock.millis();
        AtomicBoolean allow = new AtomicBoolean(false);

        lastLoggedByKey.compute(key, (k, lastLogged) -> {
            if (lastLogged == null || now - lastLogged >= intervalMillis) {
                allow.set(true);
                return now;
            }
            return lastLogged;
        });

        return allow.get();
    }
}
