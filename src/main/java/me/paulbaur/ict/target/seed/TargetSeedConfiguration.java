package me.paulbaur.ict.target.seed;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.logging.LoggingContext;
import me.paulbaur.ict.target.manager.TargetManager;
import org.springframework.context.annotation.Configuration;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Map;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TargetSeedConfiguration {

    private final TargetSeedLoader targetSeedLoader;
    private final TargetManager targetManager;

    @PostConstruct
    public void initializeTargets() {
        String seedRunId = UUID.randomUUID().toString();
        try (LoggingContext ignored = LoggingContext.withValues(Map.of("seedRunId", seedRunId))) {
            log.info("Initializing targets from seeds", kv("seedRunId", seedRunId));
            targetManager.initializeFromSeeds(targetSeedLoader.loadSeeds());
        }
    }
}
