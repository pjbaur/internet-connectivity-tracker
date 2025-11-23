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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ElasticProbeRepository implements ProbeRepository {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ElasticProbeRepository.class);

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
            log.error(
                    "Failed to index probe result",
                    kv("index", index),
                    kv("targetId", result.targetId()),
                    kv("host", result.targetHost()),
                    kv("timestamp", result.timestamp()),
                    ex
            );
            // Wrap lower-level exception in repository-specific unchecked exception
            throw new ProbeRepositoryException("Failed to index probe result for target " + result.targetId(), ex);
        }
    }

    /**
     * Find the most recent probe results for a specific target.
     *
     * <p>This method executes a match query against the targetId.keyword field to
     * ensure exact matching of the provided targetId. Results are sorted by the
     * document {@code timestamp} field in descending order (newest first) and limited
     * to {@code limit} entries.</p>
     *
     * @param targetId the target identifier to filter by (must not be null)
     * @param limit maximum number of results to return
     * @return a list of ProbeResult objects (empty list on errors)
     */
    @Override
    public List<ProbeResult> findRecent(String targetId, int limit) {
        try {
            // Use a match query against the keyword sub-field so exact target id matches work
            SearchRequest request = new SearchRequest.Builder()
                    .index(index)
                    .query(q -> q
                            .match(m -> m
                                    .field("targetId.keyword")
                                    .query(targetId)
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

            return extractHits(response);

        } catch (Exception ex) {
            log.error(
                    "Failed to fetch recent probe results",
                    kv("index", index),
                    kv("targetId", targetId),
                    kv("limit", limit),
                    ex
            );
            throw new ProbeRepositoryException("Failed to fetch recent probe results for target " + targetId, ex);
        }
    }


    /**
     * Find probe results for a target within a time range.
     *
     * <p>Performs a boolean query combining an exact match on the {@code targetId.keyword}
     * field and a date range on the {@code timestamp} field. The start and end
     * Instants are converted to ISO-8601 strings for the range boundaries. Results are
     * sorted by {@code timestamp} descending (newest first). A hard size limit is set
     * to 5000 to protect the cluster from excessively large queries; callers that
     * expect more results should implement paging.</p>
     *
     * @param targetId the target identifier to filter by
     * @param start inclusive start of the time range (Instant)
     * @param end inclusive end of the time range (Instant)
     * @return a list of ProbeResult objects within the specified range (empty on errors)
     */
    @Override
    public List<ProbeResult> findBetween(String targetId, Instant start, Instant end) {
        try {
            // Build a date range query using ISO-8601 strings for the start/end instants
            Query rangeQuery = Query.of(q -> q
                    .range(r -> r
                            .field("timestamp")
                            .gte(JsonData.of(start.toString()))
                            .lte(JsonData.of(end.toString()))
                    )
            );

            SearchRequest request = new SearchRequest.Builder()
                    .index(index)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m.match(mt -> mt.field("targetId.keyword").query(targetId)))
                                    .must(rangeQuery)
                            )
                    )
                    .sort(s -> s.field(f -> f.field("timestamp").order(SortOrder.Desc)))
                    // allow a reasonably large window; callers should page if they expect more
                    .size(5000)
                    .build();

            SearchResponse<ProbeResult> response =
                    client.search(request, ProbeResult.class);

            return extractHits(response);

        } catch (Exception ex) {
            log.error(
                    "Failed to fetch probe history",
                    kv("index", index),
                    kv("targetId", targetId),
                    kv("rangeStart", start),
                    kv("rangeEnd", end),
                    ex
            );
            throw new ProbeRepositoryException("Failed to fetch history for target " + targetId + " between " + start + " - " + end, ex);
        }
    }

    @Override
    public Optional<ProbeResult> findLatest() {
        try {
            SearchResponse<ProbeResult> response = client.search(s -> s
                    .index(this.index)
                    .size(1)
                    .sort(sort -> sort
                            .field(f -> f
                                    .field("timestamp")
                                    .order(SortOrder.Desc)
                            ))
                    , ProbeResult.class);

            return extractFirst(response);

        } catch (Exception ex) {
            log.error("Failed to fetch latest probe result", kv("index", index), ex);
            throw new ProbeRepositoryException("Failed to fetch latest probe result", ex);
        }
    }

    // Helper to parse SearchResponse into List<ProbeResult>
    private List<ProbeResult> extractHits(SearchResponse<ProbeResult> response) {
        if (response == null || response.hits() == null || response.hits().hits().isEmpty()) {
            return List.of();
        }
        return response.hits().hits().stream()
                .map(h -> h.source())
                .toList();
    }

    // Helper to extract the first hit as Optional
    private Optional<ProbeResult> extractFirst(SearchResponse<ProbeResult> response) {
        List<ProbeResult> hits = extractHits(response);
        if (hits.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(hits.get(0));
    }
}
