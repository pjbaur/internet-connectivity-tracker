package me.paulbaur.ict.target.seed;

import java.util.List;

/**
 * Loads target seed definitions from a configured source.
 */
public interface TargetSeedLoader {

    /**
     * Load target definitions from the underlying seed source.
     *
     * @return immutable list of seed definitions
     * @throws TargetSeedException if the seeds cannot be loaded or parsed
     */
    List<TargetDefinition> loadSeeds();
}
