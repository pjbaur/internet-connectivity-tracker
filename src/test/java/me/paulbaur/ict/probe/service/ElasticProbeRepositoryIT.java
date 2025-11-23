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
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ElasticProbeRepositoryIT {

    @DynamicPropertySource
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
    void saveAndRetrieveRecent() throws Exception {
        ProbeResult r = new ProbeResult(
                Instant.now(),
                "t1",
                "localhost",
                12L,
                "cycle-it",
                ProbeStatus.UP,
                ProbeMethod.TCP,
                null
        );

        repo.save(r);

        // Allow Elasticsearch to refresh so documents are visible to search
        Thread.sleep(1100);

        List<ProbeResult> results = repo.findRecent("t1", 10);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).targetId()).isEqualTo("t1");
    }

    @Test
    void queryBetweenTimestamps() throws Exception {
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now().plusSeconds(60);

        ProbeResult r = new ProbeResult(
                Instant.now(),
                "t1",
                "localhost",
                20L,
                "cycle-it",
                ProbeStatus.DOWN,
                ProbeMethod.TCP,
                "timeout"
        );

        repo.save(r);

        // Allow Elasticsearch to refresh so documents are visible to search
        Thread.sleep(1100);

        List<ProbeResult> results = repo.findBetween("t1", start, end);

        assertThat(results).isNotEmpty();
    }

    @Test
    void findRecent_orderingAndLimitEnforced() throws Exception {
        String target = "t-order";
        Instant now = Instant.now();

        ProbeResult newest = new ProbeResult(now, target, "host", 5L, "cycle-order", ProbeStatus.UP, ProbeMethod.TCP, null);
        ProbeResult mid = new ProbeResult(now.minusSeconds(10), target, "host", 6L, "cycle-order", ProbeStatus.UP, ProbeMethod.TCP, null);
        ProbeResult oldest = new ProbeResult(now.minusSeconds(20), target, "host", 7L, "cycle-order", ProbeStatus.UP, ProbeMethod.TCP, null);

        repo.save(oldest);
        repo.save(mid);
        repo.save(newest);

        // Allow Elasticsearch to refresh so documents are visible to search
        Thread.sleep(1100);

        List<ProbeResult> results = repo.findRecent(target, 2);

        assertThat(results).hasSize(2);
        // newest first
        assertThat(results.get(0).timestamp()).isAfter(results.get(1).timestamp());
        assertThat(results.get(0).timestamp()).isEqualTo(newest.timestamp());
    }

    @Test
    void findBetween_timeRangeFiltering() throws Exception {
        String target = "t-range";
        Instant now = Instant.now();

        ProbeResult a = new ProbeResult(now.minusSeconds(30), target, "host", 1L, "cycle-range", ProbeStatus.UP, ProbeMethod.TCP, null);
        ProbeResult b = new ProbeResult(now.minusSeconds(20), target, "host", 2L, "cycle-range", ProbeStatus.UP, ProbeMethod.TCP, null);
        ProbeResult c = new ProbeResult(now.minusSeconds(10), target, "host", 3L, "cycle-range", ProbeStatus.UP, ProbeMethod.TCP, null);
        ProbeResult d = new ProbeResult(now, target, "host", 4L, "cycle-range", ProbeStatus.UP, ProbeMethod.TCP, null);

        repo.save(a);
        repo.save(b);
        repo.save(c);
        repo.save(d);

        // Allow Elasticsearch to refresh so documents are visible to search
        Thread.sleep(1100);

        Instant start = now.minusSeconds(25);
        Instant end = now.minusSeconds(5);

        List<ProbeResult> results = repo.findBetween(target, start, end);

        // Should include b and c only
        assertThat(results).hasSize(2);
        assertThat(results).allSatisfy(rr -> assertThat(rr.timestamp()).isAfterOrEqualTo(start).isBeforeOrEqualTo(end));
    }
}
