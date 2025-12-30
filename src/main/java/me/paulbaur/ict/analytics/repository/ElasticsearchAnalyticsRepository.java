package me.paulbaur.ict.analytics.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.analytics.domain.LatencyMetrics;
import me.paulbaur.ict.analytics.domain.StateChange;
import me.paulbaur.ict.analytics.domain.TimeSeriesDataPoint;
import me.paulbaur.ict.analytics.domain.UptimeMetrics;
import me.paulbaur.ict.common.model.ProbeStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Repository for analytics aggregations using Elasticsearch.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchAnalyticsRepository {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${ict.elasticsearch.index}")
    private String indexPattern;

    /**
     * Calculate uptime metrics for a target within a time range.
     */
    public UptimeMetrics calculateUptime(String targetId, Instant start, Instant end) throws IOException {
        log.debug("Calculating uptime metrics", kv("targetId", targetId), kv("start", start), kv("end", end));

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexPattern + "*")
                .size(0)
                .query(buildRangeQuery(targetId, start, end))
                .aggregations("by_status", Aggregation.of(a -> a
                        .terms(TermsAggregation.of(t -> t
                                .field("status.keyword")
                        ))
                ))
        );

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

        long totalProbes = response.hits().total().value();
        long successfulProbes = 0;
        long failedProbes = 0;

        StringTermsAggregate byStatus = response.aggregations().get("by_status").sterms();
        for (StringTermsBucket bucket : byStatus.buckets().array()) {
            if ("UP".equals(bucket.key().stringValue())) {
                successfulProbes = bucket.docCount();
            } else if ("DOWN".equals(bucket.key().stringValue())) {
                failedProbes = bucket.docCount();
            }
        }

        return UptimeMetrics.calculate(targetId, start, end, totalProbes, successfulProbes, failedProbes);
    }

    /**
     * Calculate latency metrics for a target within a time range.
     */
    public LatencyMetrics calculateLatency(String targetId, Instant start, Instant end) throws IOException {
        log.debug("Calculating latency metrics", kv("targetId", targetId), kv("start", start), kv("end", end));

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexPattern + "*")
                .size(0)
                .query(buildLatencyQuery(targetId, start, end))
                .aggregations("latency_stats", Aggregation.of(a -> a
                        .stats(StatsAggregation.of(st -> st
                                .field("latencyMs")
                        ))
                ))
        );

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

        StatsAggregate latencyStats = response.aggregations().get("latency_stats").stats();

        return new LatencyMetrics(
                targetId,
                start,
                end,
                latencyStats.avg(),
                latencyStats.min(),
                latencyStats.max(),
                latencyStats.count()
        );
    }

    /**
     * Find state changes for a target within a time range.
     * This is a simplified implementation that looks for consecutive documents with different statuses.
     */
    public List<StateChange> findStateChanges(String targetId, Instant start, Instant end, int limit) throws IOException {
        log.debug("Finding state changes", kv("targetId", targetId), kv("start", start), kv("end", end));

        // For a full implementation, this would need a more complex query or
        // post-processing logic to detect actual state transitions.
        // This is a placeholder that returns an empty list.
        // A production implementation might use a scripted metric aggregation
        // or retrieve sorted documents and detect changes in application code.

        return new ArrayList<>();
    }

    /**
     * Calculate time series data points with bucketed metrics.
     */
    public List<TimeSeriesDataPoint> calculateTimeSeries(String targetId, Instant start, Instant end, String interval) throws IOException {
        log.debug("Calculating time series", kv("targetId", targetId), kv("start", start), kv("end", end), kv("interval", interval));

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexPattern + "*")
                .size(0)
                .query(buildRangeQuery(targetId, start, end))
                .aggregations("time_buckets", Aggregation.of(a -> a
                        .dateHistogram(DateHistogramAggregation.of(dh -> dh
                                .field("timestamp")
                                .calendarInterval(CalendarInterval.Hour) // Could be parameterized
                        ))
                        .aggregations("avg_latency", Aggregation.of(agg -> agg
                                .avg(AverageAggregation.of(avg -> avg.field("latencyMs")))
                        ))
                        .aggregations("by_status", Aggregation.of(agg -> agg
                                .terms(TermsAggregation.of(t -> t.field("status.keyword")))
                        ))
                ))
        );

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

        List<TimeSeriesDataPoint> dataPoints = new ArrayList<>();
        DateHistogramAggregate timeBuckets = response.aggregations().get("time_buckets").dateHistogram();

        for (DateHistogramBucket bucket : timeBuckets.buckets().array()) {
            long totalCount = bucket.docCount();
            Double avgLatency = bucket.aggregations().get("avg_latency").avg().value();

            // Count successful probes
            long successfulCount = 0;
            StringTermsAggregate statusBuckets = bucket.aggregations().get("by_status").sterms();
            for (StringTermsBucket statusBucket : statusBuckets.buckets().array()) {
                if ("UP".equals(statusBucket.key().stringValue())) {
                    successfulCount = statusBucket.docCount();
                    break;
                }
            }

            double uptimePercentage = totalCount > 0 ? (successfulCount * 100.0 / totalCount) : 0.0;

            dataPoints.add(new TimeSeriesDataPoint(
                    Instant.ofEpochMilli(Long.parseLong(bucket.keyAsString())),
                    avgLatency,
                    totalCount,
                    successfulCount,
                    uptimePercentage
            ));
        }

        return dataPoints;
    }

    /**
     * Build a query for a target within a time range.
     */
    private Query buildRangeQuery(String targetId, Instant start, Instant end) {
        return Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                        .filter(Query.of(f -> f
                                .term(TermQuery.of(t -> t
                                        .field("targetId.keyword")
                                        .value(targetId)
                                ))
                        ))
                        .filter(Query.of(f -> f
                                .range(RangeQuery.of(r -> r
                                        .field("timestamp")
                                        .gte(JsonData.of(start.toString()))
                                        .lte(JsonData.of(end.toString()))
                                ))
                        ))
                ))
        );
    }

    /**
     * Build a query for latency calculation (only include probes with latency data).
     */
    private Query buildLatencyQuery(String targetId, Instant start, Instant end) {
        return Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                        .filter(Query.of(f -> f
                                .term(TermQuery.of(t -> t
                                        .field("targetId.keyword")
                                        .value(targetId)
                                ))
                        ))
                        .filter(Query.of(f -> f
                                .range(RangeQuery.of(r -> r
                                        .field("timestamp")
                                        .gte(JsonData.of(start.toString()))
                                        .lte(JsonData.of(end.toString()))
                                ))
                        ))
                        .filter(Query.of(f -> f
                                .exists(e -> e.field("latencyMs"))
                        ))
                ))
        );
    }
}
