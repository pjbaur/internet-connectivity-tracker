package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.store.TargetRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinTargetSelector {

    private final TargetRepository targetRepository;
    private final AtomicInteger counter = new AtomicInteger(0);

    public RoundRobinTargetSelector(TargetRepository targetRepository) {
        this.targetRepository = targetRepository;
    }

    public Target nextTarget() {
        List<Target> targets = targetRepository.findAll();

        if (targets.isEmpty()) {
            return null;
        }

        int index = Math.abs(counter.getAndIncrement() % targets.size());
        return targets.get(index);
    }
}
