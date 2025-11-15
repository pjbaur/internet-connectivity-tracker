package me.paulbaur.ict.probe.service;

import me.paulbaur.ict.TestElasticsearchContainer;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ElasticProbeRepositoryIT {

    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("ict.elasticsearch.host", () -> TestElasticsearchContainer.ES.getHost());
        registry.add("ict.elasticsearch.port", () -> TestElasticsearchContainer.ES.getFirstMappedPort());
        registry.add("ict.elasticsearch.index", () -> "probe-results");
    }

    @Autowired
    ProbeRepository repo;

    @BeforeEach
    void setup() {
        // Index auto-created (MVP)
    }

    @Test
    void saveAndRetrieveRecent() {
        ProbeResult r = new ProbeResult(
                Instant.now(),
                "t1",
                "localhost",
                12L,
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );

        repo.save(r);

        List<ProbeResult> results = repo.findRecent("t1", 10);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).targetId()).isEqualTo("t1");
    }

    @Test
    void queryBetweenTimestamps() {
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now().plusSeconds(60);

        ProbeResult r = new ProbeResult(
                Instant.now(),
                "t1",
                "localhost",
                20L,
                ProbeStatus.DOWN,
                ProbeMethod.TCP,
                "timeout"
        );

        repo.save(r);

        List<ProbeResult> results = repo.findBetween("t1", start, end);

        assertThat(results).isNotEmpty();
    }
}
