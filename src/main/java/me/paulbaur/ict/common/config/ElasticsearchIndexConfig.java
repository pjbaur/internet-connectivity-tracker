package me.paulbaur.ict.common.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.ilm.*;
import co.elastic.clients.elasticsearch.indices.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures Elasticsearch Index Lifecycle Management (ILM) policies and index templates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexConfig {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${ict.elasticsearch.index}")
    private String indexPattern;

    @Value("${ict.elasticsearch.ilm.policy-name:probe-results-policy}")
    private String policyName;

    @Value("${ict.elasticsearch.ilm.hot-phase-days:30}")
    private int hotPhaseDays;

    @Value("${ict.elasticsearch.ilm.warm-phase-days:90}")
    private int warmPhaseDays;

    @Value("${ict.elasticsearch.ilm.delete-phase-days:365}")
    private int deletePhaseDays;

    /**
     * Initialize index template when the application starts.
     * Note: ILM policy configuration should be done manually through Elasticsearch API or Kibana.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndexLifecycle() {
        try {
            createIndexTemplate();
            log.info("Elasticsearch index template initialized successfully");
            log.info("ILM policy '{}' should be configured manually with hot-phase-days={}, warm-phase-days={}, delete-phase-days={}",
                    policyName, hotPhaseDays, warmPhaseDays, deletePhaseDays);
        } catch (IOException e) {
            log.warn("Failed to initialize Elasticsearch index template: {}", e.getMessage());
            log.debug("Index template initialization error details", e);
        }
    }

    /**
     * Create index template for probe results.
     */
    private void createIndexTemplate() throws IOException {
        String templateName = indexPattern + "-template";
        log.info("Creating index template: {}", templateName);

        // Check if template already exists
        try {
            ExistsIndexTemplateRequest existsRequest = ExistsIndexTemplateRequest.of(e -> e.name(templateName));
            if (elasticsearchClient.indices().existsIndexTemplate(existsRequest).value()) {
                log.info("Index template '{}' already exists, skipping creation", templateName);
                return;
            }
        } catch (Exception e) {
            log.debug("Error checking for existing template, will attempt creation", e);
        }

        // Define index settings (basic settings without ILM for now)
        IndexSettings settings = IndexSettings.of(s -> s
                .numberOfShards("1")
                .numberOfReplicas("1")
        );

        // Define mappings for probe results
        Map<String, Property> properties = new HashMap<>();
        properties.put("timestamp", Property.of(pr -> pr.date(d -> d)));
        properties.put("targetId", Property.of(pr -> pr.keyword(k -> k)));
        properties.put("targetHost", Property.of(pr -> pr.keyword(k -> k)));
        properties.put("status", Property.of(pr -> pr.keyword(k -> k)));
        properties.put("latencyMs", Property.of(pr -> pr.double_(db -> db)));
        properties.put("method", Property.of(pr -> pr.keyword(k -> k)));
        properties.put("probeCycleId", Property.of(pr -> pr.keyword(k -> k)));
        properties.put("errorMessage", Property.of(pr -> pr.text(t -> t)));

        TypeMapping mappings = TypeMapping.of(m -> m.properties(properties));

        PutIndexTemplateRequest request = PutIndexTemplateRequest.of(r -> r
                .name(templateName)
                .indexPatterns(indexPattern + "*")
                .template(t -> t
                        .settings(settings)
                        .mappings(mappings)
                )
        );

        elasticsearchClient.indices().putIndexTemplate(request);
        log.info("Index template '{}' created successfully", templateName);
    }
}
