package me.paulbaur.ict.target.service;

import me.paulbaur.ict.target.domain.Target;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class TargetServiceImplTest {

    @Test
    void createDelegatesToRepository() {
        TargetRepository repo = mock(TargetRepository.class);
        TargetServiceImpl svc = new TargetServiceImpl(repo);

        Target t = new Target(UUID.randomUUID(), "label", "host", 80);
        svc.create(t);

        verify(repo).save(t);
    }
}
