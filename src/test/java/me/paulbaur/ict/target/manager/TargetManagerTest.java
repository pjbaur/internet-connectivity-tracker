package me.paulbaur.ict.target.manager;

import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.manager.TargetManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TargetManagerTest {

    private TargetManager targetManager;

    @BeforeEach
    void setUp() {
        targetManager = new TargetManager();
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
}
