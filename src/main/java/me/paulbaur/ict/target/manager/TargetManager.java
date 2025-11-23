package me.paulbaur.ict.target.manager;

import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.seed.TargetDefinition;
import me.paulbaur.ict.target.store.TargetRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class TargetManager {

    private final Map<String, Target> targets = new LinkedHashMap<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final TargetRepository targetRepository;

    public TargetManager(TargetRepository targetRepository) {
        this.targetRepository = Objects.requireNonNull(targetRepository, "targetRepository must not be null");
    }

    public synchronized List<Target> listTargets() {
        return new ArrayList<>(targets.values());
    }

    public synchronized Target addTarget(Target target) {
        targets.put(target.getId().toString(), target);
        return target;
    }

    public synchronized void removeTarget(String id) {
        if (!targets.containsKey(id)) {
            return;
        }

        // Capture keys and size before modification
        List<String> keysBefore = new ArrayList<>(targets.keySet());
        int prevSize = keysBefore.size();
        int removedIndex = keysBefore.indexOf(id);

        // Remove the target
        targets.remove(id);

        // if no targets left, reset index
        if (targets.isEmpty()) {
            currentIndex.set(0);
            return;
        }

        // Compute current next position relative to the previous size
        int pos = (prevSize > 0) ? (currentIndex.get() % prevSize) : 0;

        // If the removed element was before the current next position,
        // decrement the numeric index so the logical next element stays the same.
        if (removedIndex >= 0 && removedIndex < pos) {
            currentIndex.decrementAndGet();
            if (currentIndex.get() < 0) {
                currentIndex.set(0);
            }
        }
        // If removedIndex == pos or removedIndex > pos -> no change needed
    }

    public synchronized Optional<Target> nextTargetRoundRobin() {
        if (targets.isEmpty()) {
            return Optional.empty();
        }

        List<Target> currentTargets = listTargets();
        if (currentTargets.isEmpty()) {
            return Optional.empty();
        }

        int size = currentTargets.size();
        int index = currentIndex.getAndIncrement();

        // Handle potential overflow and reset if it exceeds integer max value
        if (index >= Integer.MAX_VALUE - 100) { // Arbitrary threshold to prevent overflow
            currentIndex.set(0);
        }

        return Optional.of(currentTargets.get(index % size));
    }

    /**
     * Seed targets into the repository, ensuring host/port uniqueness.
     */
    public void initializeFromSeeds(List<TargetDefinition> seeds) {
        if (seeds == null || seeds.isEmpty()) {
            log.info("No target seeds provided; skipping initialization");
            return;
        }

        Map<String, Target> existingByKey = new HashMap<>();
        targetRepository.findAll().forEach(target ->
                existingByKey.put(targetKey(target.getHost(), target.getPort()), target)
        );

        for (TargetDefinition seed : seeds) {
            if (seed == null) {
                log.warn("Encountered null seed entry; skipping");
                continue;
            }

            String host = seed.host();
            Integer port = seed.port();

            if (host == null || host.isBlank()) {
                log.warn("Skipping seed with missing host");
                continue;
            }
            if (port == null || port <= 0) {
                log.warn("Skipping seed with invalid port for host {}", host);
                continue;
            }

            String key = targetKey(host, port);
            if (existingByKey.containsKey(key)) {
                log.info("Target already exists; skipping seed {}", Map.of("host", host, "port", port));
                continue;
            }

            String label = seed.label() != null ? seed.label() : host;
            Target newTarget = new Target(UUID.randomUUID(), label, host, port);
            targetRepository.save(newTarget);
            addTarget(newTarget);
            existingByKey.put(key, newTarget);

            log.info("Seeding target: {}", Map.of("host", host, "port", port));
        }
    }

    private String targetKey(String host, int port) {
        return host.toLowerCase(Locale.ROOT) + ":" + port;
    }
}
