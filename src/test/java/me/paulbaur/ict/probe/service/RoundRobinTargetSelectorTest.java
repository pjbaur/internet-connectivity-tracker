package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.store.TargetRepository;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoundRobinTargetSelectorTest {

    @Test
    void cyclesThroughTargetsInOrder() {
        TargetRepository repo = mock(TargetRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                new Target(UUID.randomUUID(), "A", "a.com", 80),
                new Target(UUID.randomUUID(), "B", "b.com", 80)
        ));

        RoundRobinTargetSelector selector = new RoundRobinTargetSelector(repo);

        Target t1 = selector.nextTarget();
        Target t2 = selector.nextTarget();
        Target t3 = selector.nextTarget();

        assertThat(t1.getLabel()).isEqualTo("A");
        assertThat(t2.getLabel()).isEqualTo("B");
        assertThat(t3.getLabel()).isEqualTo("A"); // wraparound
    }

    @Test
    void returnsNullWhenNoTargets() {
        TargetRepository repo = mock(TargetRepository.class);
        when(repo.findAll()).thenReturn(List.of());

        RoundRobinTargetSelector selector = new RoundRobinTargetSelector(repo);

        assertThat(selector.nextTarget()).isNull();
    }
}
