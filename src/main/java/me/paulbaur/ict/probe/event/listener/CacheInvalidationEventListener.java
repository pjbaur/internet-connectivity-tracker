package me.paulbaur.ict.probe.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.probe.event.ProbeResultEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Event listener that invalidates cache entries when new probe results arrive.
 * This ensures that cached data remains fresh and reflects the latest probe results.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationEventListener {

    private final CacheManager cacheManager;

    /**
     * Handle probe result events by invalidating relevant cache entries.
     *
     * @param event the probe result event
     */
    @EventListener
    public void handleProbeResultEvent(ProbeResultEvent event) {
        String targetId = event.getResult().targetId();

        try {
            // Evict cache entries for this target
            evictCacheForTarget(targetId);

            log.debug(
                    "Invalidated cache for target after new probe result",
                    kv("targetId", targetId),
                    kv("status", event.getResult().status())
            );
        } catch (Exception e) {
            log.warn(
                    "Failed to invalidate cache for target",
                    kv("targetId", targetId),
                    kv("error", e.getMessage()),
                    e
            );
            // Don't rethrow - cache invalidation failure shouldn't break event processing
        }
    }

    /**
     * Evict cache entries for a specific target.
     *
     * @param targetId the target ID
     */
    private void evictCacheForTarget(String targetId) {
        // Evict from probe-results cache
        Cache probeResultsCache = cacheManager.getCache("probe-results");
        if (probeResultsCache != null) {
            probeResultsCache.evict(targetId);
            log.debug("Evicted probe-results cache entry", kv("targetId", targetId));
        }

        // Evict from target-status cache
        Cache targetStatusCache = cacheManager.getCache("target-status");
        if (targetStatusCache != null) {
            targetStatusCache.evict(targetId);
            log.debug("Evicted target-status cache entry", kv("targetId", targetId));
        }
    }
}
