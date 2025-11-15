package me.paulbaur.ict.probe.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;

import me.paulbaur.ict.probe.domain.ProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class ElasticProbeRepository implements ProbeRepository {
    private static final Logger log = LoggerFactory.getLogger(ElasticProbeRepository.class);

    private final ElasticsearchClient client;
    private final String index;

    public ElasticProbeRepository(
            ElasticsearchClient client,
            @Value("${ict.elasticsearch.index:probe-results}") String index) {
        this.client = client;
        this.index = index;
    }

    @Override
    public void save(ProbeResult result) {
        try {
            IndexRequest<ProbeResult> req = new IndexRequest.Builder<ProbeResult>()
                    .index(index)
                    .document(result)
                    .build();

            client.index(req);
        } catch (Exception ex) {
            log.error("Failed to index probe result for target {}", result.targetId(), ex);
            // Continue running. If we rethrow, the ProbeScheduler stops permanently.
        }
    }

    @Override
    public List<ProbeResult> findRecent(String targetId, int limit) {
        try {
            SearchRequest request = new SearchRequest.Builder()
                    .index(index)
                    .query(q -> q
                            .term(t -> t
                                    .field("targetId")
                                    .value(targetId)
                            )
                    )
                    .sort(s -> s
                            .field(f -> f
                                    .field("timestamp")
                                    .order(SortOrder.Desc)
                            )
                    )
                    .size(limit)
                    .build();

            SearchResponse<ProbeResult> response =
                    client.search(request, ProbeResult.class);

            return response.hits().hits().stream()
                    .map(h -> h.source())
                    .toList();

        } catch (Exception ex) {
            log.error("Failed to fetch recent probe results for target {}", targetId, ex);
            return List.of();
        }
    }


    @Override
    public List<ProbeResult> findBetween(String targetId, Instant start, Instant end) {
        try {
            Query rangeQuery = Query.of(q -> q
                    .range(r -> r
                            .field("timestamp")
                            .gte(JsonData.of(start.toEpochMilli()))
                            .lte(JsonData.of(end.toEpochMilli()))
                    )
            );

            SearchRequest request = new SearchRequest.Builder()
                    .index(index)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m.term(t -> t.field("targetId").value(targetId)))
                                    .must(rangeQuery)
                            )
                    )
                    .sort(s -> s.field(f -> f.field("timestamp").order(SortOrder.Asc)))
                    .size(5000)
                    .build();

            SearchResponse<ProbeResult> response =
                    client.search(request, ProbeResult.class);

            return response.hits().hits().stream()
                    .map(h -> h.source())
                    .toList();

        } catch (Exception ex) {
            log.error("Failed to fetch history for {} between {} - {}", targetId, start, end, ex);
            return List.of();
        }
    }
}
