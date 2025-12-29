package me.paulbaur.ict.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class ProbeSchedulerHealthIndicator implements HealthIndicator {

    private static final Duration HEALTH_THRESHOLD = Duration.ofSeconds(10);

    private final AtomicReference<Instant> lastExecutionTime = new AtomicReference<>(Instant.now());
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);

    @Override
    public Health health() {
        Instant lastExecution = lastExecutionTime.get();
        Duration timeSinceLastExecution = Duration.between(lastExecution, Instant.now());
        long total = totalExecutions.get();
        long failed = failedExecutions.get();

        Health.Builder builder;
        if (timeSinceLastExecution.compareTo(HEALTH_THRESHOLD) > 0) {
            builder = Health.down()
                    .withDetail("reason", "No probe execution in last " + HEALTH_THRESHOLD.toSeconds() + " seconds");
        } else {
            builder = Health.up();
        }

        return builder
                .withDetail("lastExecutionTime", lastExecution.toString())
                .withDetail("secondsSinceLastExecution", timeSinceLastExecution.toSeconds())
                .withDetail("totalExecutions", total)
                .withDetail("failedExecutions", failed)
                .withDetail("successRate", total > 0 ? String.format("%.2f%%", ((total - failed) * 100.0 / total)) : "N/A")
                .build();
    }

    public void recordExecution() {
        lastExecutionTime.set(Instant.now());
        totalExecutions.incrementAndGet();
    }

    public void recordFailure() {
        failedExecutions.incrementAndGet();
    }
}
