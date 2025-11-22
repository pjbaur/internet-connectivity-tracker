package me.paulbaur.ict.target.store;

import me.paulbaur.ict.target.domain.Target;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TargetRepository {

    List<Target> findAll();

    Optional<Target> findById(UUID id);

    Target save(Target target);

    boolean delete(UUID id);
}
