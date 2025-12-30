package me.paulbaur.ict.probe.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import me.paulbaur.ict.common.logging.LogCapture;
import me.paulbaur.ict.common.metrics.ProbeMetrics;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategy;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategyFactory;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.store.TargetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)

class ProbeLoggingTest {

    private static final UUID TARGET_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final Target TARGET = new Target(TARGET_ID, "Logging Target", "example.com", 8080);

    @Mock
    private ProbeMetrics probeMetrics;

    private ProbeStrategyStub probeStrategy;
    private ProbeStrategyFactoryStub probeStrategyFactory;
    private RecordingProbeRepository probeRepository;
    private TargetRepositoryStub targetRepository;
    private RoundRobinTargetSelectorStub selector;

    private ProbeServiceImpl probeService;

    @BeforeEach
    void setUp() {
        probeStrategy = new ProbeStrategyStub();
        probeStrategyFactory = new ProbeStrategyFactoryStub(probeStrategy);
        probeRepository = new RecordingProbeRepository();
        targetRepository = new TargetRepositoryStub();
        selector = new RoundRobinTargetSelectorStub();

        probeService = new ProbeServiceImpl(selector, probeStrategyFactory, probeRepository, targetRepository, probeMetrics);
    }

    @Test
    void probe_success_logsStructuredKeysAndMdc() {
        ProbeResult success = new ProbeResult(
                Instant.parse("2024-01-01T00:00:00Z"),
                TARGET_ID.toString(),
                TARGET.getHost(),
                42L,
                "strategy-cycle",
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );
        probeStrategy.setNextResult(success);

        try (LogCapture capture = LogCapture.capture(ProbeServiceImpl.class, Level.DEBUG)) {
            ProbeResult result = probeService.probe(TARGET);

            ILoggingEvent event = capture.firstMatching(e ->
                    "Probe completed for target".equals(e.getFormattedMessage())
            ).orElseThrow();

            Map<String, Object> args = LogCapture.structuredArguments(event);
            assertThat(args)
                    .containsEntry("host", TARGET.getHost())
                    .containsEntry("port", TARGET.getPort())
                    .containsEntry("status", ProbeStatus.UP.name())
                    .containsEntry("method", ProbeMethod.TCP.name());

            assertThat(args.get("latencyMs")).isInstanceOf(Number.class);
            assertThat(((Number) args.get("latencyMs")).longValue()).isEqualTo(result.latencyMs());

            assertThat(event.getMDCPropertyMap())
                    .containsEntry("targetId", TARGET_ID.toString())
                    .containsEntry("probeCycleId", result.probeCycleId());
        }
    }

    @Test
    void probe_failure_logsErrorWithContext() {
        RuntimeException failure = new RuntimeException("boom");
        probeStrategy.setNextException(failure);

        try (LogCapture capture = LogCapture.capture(ProbeServiceImpl.class, Level.DEBUG)) {
            probeService.probe(TARGET);

            ILoggingEvent event = capture.firstMatching(e ->
                    "Unexpected error during probe".equals(e.getFormattedMessage())
            ).orElseThrow();

            Map<String, Object> args = LogCapture.structuredArguments(event);
            assertThat(args)
                    .containsEntry("host", TARGET.getHost())
                    .containsEntry("port", TARGET.getPort())
                    .containsEntry("status", ProbeStatus.DOWN.name())
                    .containsEntry("method", ProbeMethod.TCP.name())
                    .containsEntry("error", failure.getMessage());
            assertThat(event.getThrowableProxy()).isNotNull();

            assertThat(event.getMDCPropertyMap())
                    .containsEntry("targetId", TARGET_ID.toString())
                    .containsKey("probeCycleId");
        }
    }

    @Test
    void repository_failure_logsTargetIdAndLimit() {
        ProbeServiceImpl failingService = new ProbeServiceImpl(
                selector,
                probeStrategyFactory,
                new FailingProbeRepository(),
                targetRepository,
                probeMetrics
        );

        try (LogCapture capture = LogCapture.capture(ProbeServiceImpl.class, Level.DEBUG)) {
            List<ProbeResult> results = failingService.getRecentResultsForTarget("target-123", 5);
            assertThat(results).isEmpty();

            ILoggingEvent event = capture.firstMatching(e ->
                    "Failed to retrieve recent results".equals(e.getFormattedMessage())
            ).orElseThrow();

            Map<String, Object> args = LogCapture.structuredArguments(event);
            assertThat(args)
                    .containsEntry("targetId", "target-123")
                    .containsEntry("limit", 5);
            assertThat(event.getThrowableProxy()).isNotNull();
        }
    }

    // --- Test doubles ---

    static class ProbeStrategyFactoryStub extends ProbeStrategyFactory {
        private final ProbeStrategy strategy;

        ProbeStrategyFactoryStub(ProbeStrategy strategy) {
            super(null, null);
            this.strategy = strategy;
        }

        @Override
        public ProbeStrategy getStrategy(Target target) {
            return strategy;
        }
    }

    static class ProbeStrategyStub implements ProbeStrategy {
        private ProbeResult nextResult;
        private RuntimeException nextException;

        @Override
        public ProbeResult probe(ProbeRequest request) {
            if (nextException != null) {
                throw nextException;
            }
            return nextResult;
        }

        void setNextResult(ProbeResult nextResult) {
            this.nextResult = nextResult;
        }

        void setNextException(RuntimeException nextException) {
            this.nextException = nextException;
        }
    }

    static class RecordingProbeRepository implements ProbeRepository {
        private final List<ProbeResult> saved = new ArrayList<>();

        @Override
        public void save(ProbeResult result) {
            saved.add(result);
        }

        @Override
        public Optional<ProbeResult> findLatest() {
            return Optional.empty();
        }

        @Override
        public List<ProbeResult> findRecent(String targetId, int limit) {
            return List.of();
        }

        @Override
        public List<ProbeResult> findBetween(String targetId, Instant start, Instant end) {
            return List.of();
        }

        List<ProbeResult> saved() {
            return saved;
        }
    }

    static class TargetRepositoryStub implements TargetRepository {
        private final List<Target> targets = new ArrayList<>();

        @Override
        public List<Target> findAll() {
            return List.copyOf(targets);
        }

        @Override
        public Optional<Target> findById(UUID id) {
            return targets.stream().filter(t -> t.getId().equals(id)).findFirst();
        }

        @Override
        public Target save(Target target) {
            targets.add(target);
            return target;
        }

        @Override
        public boolean delete(UUID id) {
            return targets.removeIf(t -> t.getId().equals(id));
        }
    }

    static class RoundRobinTargetSelectorStub extends RoundRobinTargetSelector {
        RoundRobinTargetSelectorStub() {
            super(null);
        }

        @Override
        public Target nextTarget() {
            return TARGET;
        }
    }

    static class FailingProbeRepository implements ProbeRepository {

        @Override
        public void save(ProbeResult result) {
            // no-op
        }

        @Override
        public Optional<ProbeResult> findLatest() {
            return Optional.empty();
        }

        @Override
        public List<ProbeResult> findRecent(String targetId, int limit) {
            throw new ProbeRepositoryException("simulated failure");
        }

        @Override
        public List<ProbeResult> findBetween(String targetId, Instant start, Instant end) {
            return List.of();
        }
    }
}
