package me.paulbaur.ict.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.analytics.domain.LatencyMetrics;
import me.paulbaur.ict.analytics.domain.StateChange;
import me.paulbaur.ict.analytics.domain.TimeSeriesDataPoint;
import me.paulbaur.ict.analytics.domain.UptimeMetrics;
import me.paulbaur.ict.analytics.repository.ElasticsearchAnalyticsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for probe analytics and metrics aggregation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProbeAnalyticsService {

    private final ElasticsearchAnalyticsRepository analyticsRepository;

    /**
     * Get uptime metrics for a target within a time range.
     * Results are cached for 5 minutes.
     */
    @Cacheable(value = "analytics", key = "'uptime-' + #targetId + '-' + #start.epochSecond + '-' + #end.epochSecond")
    public UptimeMetrics getUptimeMetrics(String targetId, Instant start, Instant end) {
        log.info("Calculating uptime metrics", kv("targetId", targetId), kv("start", start), kv("end", end));

        try {
            return analyticsRepository.calculateUptime(targetId, start, end);
        } catch (IOException e) {
            log.error("Failed to calculate uptime metrics",
                    kv("targetId", targetId),
                    kv("error", e.getMessage()),
                    e);
            throw new AnalyticsException("Failed to calculate uptime metrics for target: " + targetId, e);
        }
    }

    /**
     * Get latency metrics for a target within a time range.
     * Results are cached for 5 minutes.
     */
    @Cacheable(value = "analytics", key = "'latency-' + #targetId + '-' + #start.epochSecond + '-' + #end.epochSecond")
    public LatencyMetrics getLatencyMetrics(String targetId, Instant start, Instant end) {
        log.info("Calculating latency metrics", kv("targetId", targetId), kv("start", start), kv("end", end));

        try {
            return analyticsRepository.calculateLatency(targetId, start, end);
        } catch (IOException e) {
            log.error("Failed to calculate latency metrics",
                    kv("targetId", targetId),
                    kv("error", e.getMessage()),
                    e);
            throw new AnalyticsException("Failed to calculate latency metrics for target: " + targetId, e);
        }
    }

    /**
     * Get state changes for a target within a time range.
     * Results are cached for 5 minutes.
     */
    @Cacheable(value = "analytics", key = "'state-changes-' + #targetId + '-' + #start.epochSecond + '-' + #end.epochSecond + '-' + #limit")
    public List<StateChange> getStateChanges(String targetId, Instant start, Instant end, int limit) {
        log.info("Finding state changes",
                kv("targetId", targetId),
                kv("start", start),
                kv("end", end),
                kv("limit", limit));

        try {
            return analyticsRepository.findStateChanges(targetId, start, end, limit);
        } catch (IOException e) {
            log.error("Failed to find state changes",
                    kv("targetId", targetId),
                    kv("error", e.getMessage()),
                    e);
            throw new AnalyticsException("Failed to find state changes for target: " + targetId, e);
        }
    }

    /**
     * Get time series data for a target within a time range.
     * Results are cached for 5 minutes.
     */
    @Cacheable(value = "analytics", key = "'time-series-' + #targetId + '-' + #start.epochSecond + '-' + #end.epochSecond + '-' + #interval")
    public List<TimeSeriesDataPoint> getTimeSeries(String targetId, Instant start, Instant end, String interval) {
        log.info("Calculating time series",
                kv("targetId", targetId),
                kv("start", start),
                kv("end", end),
                kv("interval", interval));

        try {
            return analyticsRepository.calculateTimeSeries(targetId, start, end, interval);
        } catch (IOException e) {
            log.error("Failed to calculate time series",
                    kv("targetId", targetId),
                    kv("error", e.getMessage()),
                    e);
            throw new AnalyticsException("Failed to calculate time series for target: " + targetId, e);
        }
    }

    /**
     * Exception thrown when analytics operations fail.
     */
    public static class AnalyticsException extends RuntimeException {
        public AnalyticsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
