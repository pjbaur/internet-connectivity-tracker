package me.paulbaur.ict.target.manager;

import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.seed.TargetDefinition;
import me.paulbaur.ict.target.store.TargetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class TargetManagerTest {

    private TargetManager targetManager;
    private TargetRepositoryStub targetRepositoryStub;

    @BeforeEach
    void setUp() {
        targetRepositoryStub = new TargetRepositoryStub();
        targetManager = new TargetManager(targetRepositoryStub);
    }

    @Test
    void listTargets_shouldReturnEmptyList_whenNoTargets() {
        assertTrue(targetManager.listTargets().isEmpty());
    }

    @Test
    void addTarget_shouldIncreaseTargetCount() {
        Target target1 = new Target(UUID.randomUUID(), "Label 1", "host1", 80);
        targetManager.addTarget(target1);
        assertEquals(1, targetManager.listTargets().size());
        assertTrue(targetManager.listTargets().contains(target1));
    }

    @Test
    void removeTarget_shouldDecreaseTargetCount() {
        Target target1 = new Target(UUID.randomUUID(), "Label 1", "host1", 80);
        targetManager.addTarget(target1);
        assertEquals(1, targetManager.listTargets().size());

        targetManager.removeTarget(target1.getId().toString());
        assertTrue(targetManager.listTargets().isEmpty());
    }

    @Test
    void removeTarget_shouldNotFail_whenTargetDoesNotExist() {
        targetManager.removeTarget("nonExistentId");
        assertTrue(targetManager.listTargets().isEmpty());
    }

    @Test
    void nextTargetRoundRobin_shouldReturnEmpty_whenNoTargets() {
        assertTrue(targetManager.nextTargetRoundRobin().isEmpty());
    }

    @Test
    void nextTargetRoundRobin_singleTargetBehavior() {
        Target target1 = new Target(UUID.randomUUID(), "Label 1", "host1", 80);
        targetManager.addTarget(target1);

        Optional<Target> next = targetManager.nextTargetRoundRobin();
        assertTrue(next.isPresent());
        assertEquals(target1, next.get());

        // Should always return the same target as it's the only one
        next = targetManager.nextTargetRoundRobin();
        assertTrue(next.isPresent());
        assertEquals(target1, next.get());
    }

    @Test
    void nextTargetRoundRobin_multipleTargetCycling() {
        Target target1 = new Target(UUID.randomUUID(), "Label 1", "host1", 80);
        Target target2 = new Target(UUID.randomUUID(), "Label 2", "host2", 80);
        Target target3 = new Target(UUID.randomUUID(), "Label 3", "host3", 80);

        targetManager.addTarget(target1);
        targetManager.addTarget(target2);
        targetManager.addTarget(target3);

        assertEquals(target1, targetManager.nextTargetRoundRobin().get());
        assertEquals(target2, targetManager.nextTargetRoundRobin().get());
        assertEquals(target3, targetManager.nextTargetRoundRobin().get());
        assertEquals(target1, targetManager.nextTargetRoundRobin().get()); // Cycle back
        assertEquals(target2, targetManager.nextTargetRoundRobin().get());
    }

    @Test
    void removeTarget_updatesCycleCorrectly() {
        Target target1 = new Target(UUID.randomUUID(), "Label 1", "host1", 80);
        Target target2 = new Target(UUID.randomUUID(), "Label 2", "host2", 80);
        Target target3 = new Target(UUID.randomUUID(), "Label 3", "host3", 80);

        targetManager.addTarget(target1);
        targetManager.addTarget(target2);
        targetManager.addTarget(target3);

        assertEquals(target1, targetManager.nextTargetRoundRobin().get());
        assertEquals(target2, targetManager.nextTargetRoundRobin().get());

        // Remove target2
        targetManager.removeTarget(target2.getId().toString());

        // Next should be target3, not target1 (as target2 was removed)
        assertEquals(target3, targetManager.nextTargetRoundRobin().get());
        assertEquals(target1, targetManager.nextTargetRoundRobin().get()); // Cycle back to target1
        assertFalse(targetManager.listTargets().contains(target2));
    }

    @Test
    void nextTargetRoundRobin_afterRemovingAllTargets() {
        Target target1 = new Target(UUID.randomUUID(), "Label 1", "host1", 80);
        targetManager.addTarget(target1);
        targetManager.nextTargetRoundRobin(); // Get target1

        targetManager.removeTarget(target1.getId().toString());
        assertTrue(targetManager.nextTargetRoundRobin().isEmpty());
    }

    @Test
    void nextTargetRoundRobin_addAndRemoveMultipleTimes() {
        Target target1 = new Target(UUID.randomUUID(), "Label 1", "host1", 80);
        Target target2 = new Target(UUID.randomUUID(), "Label 2", "host2", 80);

        targetManager.addTarget(target1);
        assertEquals(target1, targetManager.nextTargetRoundRobin().get());

        targetManager.addTarget(target2);
        assertEquals(target2, targetManager.nextTargetRoundRobin().get());
        assertEquals(target1, targetManager.nextTargetRoundRobin().get());

        targetManager.removeTarget(target1.getId().toString());
        assertEquals(target2, targetManager.nextTargetRoundRobin().get());
        assertEquals(target2, targetManager.nextTargetRoundRobin().get()); // Only target2 left
    }

    @Test
    void initializeFromSeeds_createsMissingTargetsAndSkipsExisting() {
        Target existing = new Target(UUID.randomUUID(), "Existing", "host1", 80);
        targetRepositoryStub.save(existing);

        List<TargetDefinition> seeds = List.of(
                new TargetDefinition("Existing label", "host1", 80, null, null),
                new TargetDefinition("New Target", "host2", 443, null, null)
        );

        targetManager.initializeFromSeeds(seeds);

        List<Target> savedTargets = targetRepositoryStub.findAll();
        assertEquals(2, savedTargets.size());
        assertTrue(savedTargets.stream().anyMatch(t -> t.getHost().equals("host1") && t.getPort() == 80));
        assertTrue(savedTargets.stream().anyMatch(t -> t.getHost().equals("host2") && t.getPort() == 443));
    }

    @Test
    void initializeFromSeeds_skipsInvalidSeeds() {
        List<TargetDefinition> seeds = List.of(
                new TargetDefinition("Missing host", "", 80, null, null),
                new TargetDefinition("Invalid port", "host2", -1, null, null)
        );

        targetManager.initializeFromSeeds(seeds);

        assertTrue(targetRepositoryStub.findAll().isEmpty());
    }

    @Test
    void initializeFromSeeds_withDuplicates_createsOnceAndLogs(CapturedOutput output) {
        Target existing = new Target(UUID.randomUUID(), "Existing", "host1", 80);
        targetRepositoryStub.save(existing);

        List<TargetDefinition> seeds = List.of(
                new TargetDefinition("Existing label", "host1", 80, null, null),
                new TargetDefinition("New target", "seed.example.com", 443, "TCP", null),
                new TargetDefinition("Duplicate target", "seed.example.com", 443, null, null)
        );

        targetManager.initializeFromSeeds(seeds);

        List<Target> savedTargets = targetRepositoryStub.findAll();
        assertEquals(2, savedTargets.size());
        assertTrue(savedTargets.stream().anyMatch(t -> t.getHost().equals("host1") && t.getPort() == 80));
        assertTrue(savedTargets.stream().anyMatch(t -> t.getHost().equals("seed.example.com") && t.getPort() == 443));

        assertThat(output.getOut())
                .contains("Seeding target host=seed.example.com port=443")
                .contains("Target already exists; skipping seed host=host1 port=80")
                .contains("Target already exists; skipping seed host=seed.example.com port=443");
    }

    @Test
    void initializeFromSeeds_missingRequiredFields_logsWarningsAndSkips(CapturedOutput output) {
        List<TargetDefinition> seeds = List.of(
                new TargetDefinition("Blank host", "   ", 8080, null, null),
                new TargetDefinition("Zero port", "valid.example.com", 0, null, null)
        );

        targetManager.initializeFromSeeds(seeds);

        assertTrue(targetRepositoryStub.findAll().isEmpty());
        assertThat(output.getOut())
                .contains("Skipping seed with missing host")
                .contains("Skipping seed with invalid port for host valid.example.com");
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
            targets.removeIf(t -> t.getId().equals(target.getId()));
            targets.add(target);
            return target;
        }

        @Override
        public boolean delete(UUID id) {
            return targets.removeIf(t -> t.getId().equals(id));
        }
    }
}
