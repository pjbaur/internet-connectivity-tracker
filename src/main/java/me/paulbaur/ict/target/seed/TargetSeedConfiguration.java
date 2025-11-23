package me.paulbaur.ict.target.seed;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import me.paulbaur.ict.target.manager.TargetManager;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TargetSeedConfiguration {

    private final TargetSeedLoader targetSeedLoader;
    private final TargetManager targetManager;

    @PostConstruct
    public void initializeTargets() {
        targetManager.initializeFromSeeds(targetSeedLoader.loadSeeds());
    }
}
