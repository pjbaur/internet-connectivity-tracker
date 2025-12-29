package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.common.metrics.ProbeMetrics;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.strategy.ProbeStrategy;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)

class ProbeServiceImplTest {

    @Mock
    private ProbeMetrics probeMetrics;

    private ProbeServiceImpl probeService;

    // Test doubles (spies and stubs)
    private ProbeStrategySpy probeStrategySpy;
    private ProbeRepositoryStub probeRepositoryStub;
    private TargetRepositoryStub targetRepositoryStub;
    private RoundRobinTargetSelectorStub targetSelectorStub;

    private static final UUID TEST_TARGET_ID = UUID.randomUUID();
    private static final Target TEST_TARGET = new Target(TEST_TARGET_ID, "Test Target", "example.com", 80);

    @BeforeEach
    void setUp() {
        probeStrategySpy = new ProbeStrategySpy();
        probeRepositoryStub = new ProbeRepositoryStub();
        targetRepositoryStub = new TargetRepositoryStub();
        targetSelectorStub = new RoundRobinTargetSelectorStub();

        probeService = new ProbeServiceImpl(
                targetSelectorStub,
                probeStrategySpy,
                probeRepositoryStub,
                targetRepositoryStub,
                probeMetrics
        );
    }

    @Test
    void probe_whenStrategySucceeds_callsStrategyAndSavesResult() {
        // Arrange
        ProbeResult successResult = new ProbeResult(Instant.now(), TEST_TARGET_ID.toString(), "example.com", 100L, "cycle-from-strategy", ProbeStatus.UP, ProbeMethod.TCP, null);
        probeStrategySpy.setNextResult(successResult);

        // Act
        ProbeResult result = probeService.probe(TEST_TARGET);

        // Assert
        assertThat(result.status()).isEqualTo(ProbeStatus.UP);
        assertThat(result.probeCycleId()).isNotBlank();

        // Verify probeStrategy was called correctly
        assertThat(probeStrategySpy.getLastRequest()).isNotNull();
        assertThat(probeStrategySpy.getLastRequest().targetId()).isEqualTo(TEST_TARGET_ID.toString());
        assertThat(probeStrategySpy.getLastRequest().host()).isEqualTo("example.com");
        assertThat(probeStrategySpy.getLastRequest().probeCycleId()).isEqualTo(result.probeCycleId());

        // Verify repository save was called
        assertThat(probeRepositoryStub.getSavedResults()).hasSize(1);
        assertThat(probeRepositoryStub.getSavedResults().get(0).probeCycleId()).isEqualTo(result.probeCycleId());
    }

    @Test
    void probe_whenStrategyThrowsException_createsAndSavesFailureResult() {
        // Arrange
        RuntimeException testException = new RuntimeException("Unexpected error!");
        probeStrategySpy.setNextException(testException);

        // Act
        ProbeResult result = probeService.probe(TEST_TARGET);

        // Assert
        assertThat(result.status()).isEqualTo(ProbeStatus.DOWN);
        assertThat(result.errorMessage()).isEqualTo("unexpected error: " + testException.getMessage());
        assertThat(result.latencyMs()).isNull();
        assertThat(result.targetId()).isEqualTo(TEST_TARGET_ID.toString());
        assertThat(result.probeCycleId()).isNotBlank();

        // Verify repository save was called with the failure result
        assertThat(probeRepositoryStub.getSavedResults()).hasSize(1);
        assertThat(probeRepositoryStub.getSavedResults().get(0)).isEqualTo(result);
    }

    @Test
    void runScheduledProbes_whenTargetAvailable_probesTarget() {
        // Arrange
        targetSelectorStub.setNextTarget(TEST_TARGET);
        ProbeResult successResult = new ProbeResult(Instant.now(), TEST_TARGET_ID.toString(), "example.com", 100L, "cycle-from-strategy", ProbeStatus.UP, ProbeMethod.TCP, null);
        probeStrategySpy.setNextResult(successResult);

        // Act
        probeService.runScheduledProbes();

        // Assert
        assertThat(probeStrategySpy.getCallCount()).isEqualTo(1);
        assertThat(probeRepositoryStub.getSavedResults()).hasSize(1);
    }

    @Test
    void runScheduledProbes_whenNoTargetAvailable_doesNothing() {
        // Arrange
        targetSelectorStub.setNextTarget(null);

        // Act
        probeService.runScheduledProbes();

        // Assert
        assertThat(probeStrategySpy.getCallCount()).isZero();
        assertThat(probeRepositoryStub.getSavedResults()).isEmpty();
    }

    @Test
    void getLatestResult_whenResultPresent_returnsResult() {
        // Arrange
        ProbeResult latestResult = new ProbeResult(Instant.now(), TEST_TARGET_ID.toString(), "example.com", 50L, "cycle-latest", ProbeStatus.UP, ProbeMethod.TCP, null);
        probeRepositoryStub.setNextLatestResult(latestResult);

        // Act
        ProbeResult result = probeService.getLatestResult().orElseThrow();

        // Assert
        assertThat(result).isEqualTo(latestResult);
    }

    @Test
    void getLatestResult_whenNoResultPresent_returnsEmptyOptional() {
        // Arrange
        probeRepositoryStub.setNextLatestResult(null); // Ensure it's explicitly null

        // Act
        Optional<ProbeResult> result = probeService.getLatestResult();

        // Assert
        assertThat(result).isEmpty();
    }

    // --- Test Doubles ---

    static class ProbeStrategySpy implements ProbeStrategy {
        private ProbeRequest lastRequest;
        private ProbeResult nextResult;
        private RuntimeException nextException;
        private int callCount = 0;

        @Override
        public ProbeResult probe(ProbeRequest request) {
            this.lastRequest = request;
            this.callCount++;
            if (nextException != null) {
                throw nextException;
            }
            return nextResult;
        }

        public ProbeRequest getLastRequest() { return lastRequest; }
        public int getCallCount() { return callCount; }
        public void setNextResult(ProbeResult nextResult) { this.nextResult = nextResult; }
        public void setNextException(RuntimeException nextException) { this.nextException = nextException; }
    }

    static class ProbeRepositoryStub implements ProbeRepository {
        private final List<ProbeResult> savedResults = new ArrayList<>();
        private ProbeResult nextLatestResult;

        @Override
        public void save(ProbeResult result) {
            savedResults.add(result);
        }

        @Override
        public Optional<ProbeResult> findLatest() {
            return Optional.ofNullable(nextLatestResult);
        }

        @Override
        public List<ProbeResult> findRecent(String targetId, int limit) {
            return new ArrayList<>();
        }

        @Override
        public List<ProbeResult> findBetween(String targetId, Instant start, Instant end) {
            return List.of();
        }

        public List<ProbeResult> getSavedResults() {
            return savedResults;
        }

        public void setNextLatestResult(ProbeResult nextLatestResult) {
            this.nextLatestResult = nextLatestResult;
        }
    }

    static class TargetRepositoryStub implements TargetRepository {
        private final List<Target> targets = new ArrayList<>();

        @Override
        public List<Target> findAll() {
            return new ArrayList<>(targets);
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
        private Target nextTarget;

        public RoundRobinTargetSelectorStub() {
            super(null); // Pass null as we override the behavior
        }

        @Override
        public Target nextTarget() {
            return nextTarget;
        }

        public void setNextTarget(Target nextTarget) {
            this.nextTarget = nextTarget;
        }
    }
}
