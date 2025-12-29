package me.paulbaur.ict.common.health;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchHealthIndicator implements HealthIndicator {

    private final ElasticsearchClient elasticsearchClient;
    private static final int TIMEOUT_MS = 500;

    @Override
    public Health health() {
        try {
            HealthResponse healthResponse = elasticsearchClient.cluster()
                    .health(h -> h.timeout(t -> t.time(TIMEOUT_MS + "ms")));

            String status = healthResponse.status().jsonValue();
            String clusterName = healthResponse.clusterName();
            int numberOfNodes = healthResponse.numberOfNodes();
            int activeShards = healthResponse.activeShards();

            Health.Builder builder;
            if ("green".equals(status) || "yellow".equals(status)) {
                builder = Health.up();
            } else {
                builder = Health.down();
            }

            return builder
                    .withDetail("cluster", clusterName)
                    .withDetail("status", status)
                    .withDetail("nodes", numberOfNodes)
                    .withDetail("activeShards", activeShards)
                    .build();

        } catch (Exception ex) {
            log.error("Elasticsearch health check failed", ex);
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .withException(ex)
                    .build();
        }
    }
}
