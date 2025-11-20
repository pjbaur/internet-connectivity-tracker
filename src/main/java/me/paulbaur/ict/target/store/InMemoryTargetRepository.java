package me.paulbaur.ict.target.store;

import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.service.TargetRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTargetRepository implements TargetRepository {

    private final Map<UUID, Target> targets = new ConcurrentHashMap<>();

    @Override
    public List<Target> findAll() {
        return new ArrayList<>(targets.values());
    }

    @Override
    public Optional<Target> findById(UUID id) {
        return Optional.ofNullable(targets.get(id));
    }

    @Override
    public Target save(Target target) {
        targets.put(target.getId(), target);
        return target;
    }

    @Override
    public void delete(UUID id) {
        targets.remove(id);
    }
}
