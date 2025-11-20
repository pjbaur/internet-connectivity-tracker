package me.paulbaur.ict.target.manager;

import me.paulbaur.ict.target.domain.Target;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TargetManager {

    private final Map<String, Target> targets = new LinkedHashMap<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);

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
}
