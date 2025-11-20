package me.paulbaur.ict.target.service;

import me.paulbaur.ict.target.domain.Target;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TargetServiceImpl implements TargetService {

    private final TargetRepository repo;

    public TargetServiceImpl(TargetRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Target> findAll() {
        return repo.findAll();
    }

    @Override
    public Target create(Target target) {
        return repo.save(target);
    }

    @Override
    public void delete(UUID id) {
        repo.delete(id);
    }
}
