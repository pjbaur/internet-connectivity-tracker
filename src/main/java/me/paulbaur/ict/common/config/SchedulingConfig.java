package me.paulbaur.ict.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async probe execution thread pool.
 * Enables concurrent probe execution to improve throughput when monitoring multiple targets.
 */
@Configuration
@Slf4j
public class SchedulingConfig {

    @Value("${ict.probe.async.core-pool-size:4}")
    private int corePoolSize;

    @Value("${ict.probe.async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${ict.probe.async.queue-capacity:100}")
    private int queueCapacity;

    /**
     * Thread pool executor for async probe execution.
     * This executor is used by @Async annotated methods in ProbeScheduler.
     *
     * @return configured thread pool task executor
     */
    @Bean(name = "probeTaskExecutor")
    public Executor probeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("probe-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("Initialized probe task executor: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }
}
