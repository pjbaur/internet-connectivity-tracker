package me.paulbaur.ict.common.logging;

import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;

/**
 * Simple helper to manage MDC values in a scoped manner.
 *
 * <p>Usage:
 * <pre>
 * try (LoggingContext ignored = LoggingContext.withValues(Map.of("reqId", reqId))) {
 *     // logging here will include reqId
 * }
 * </pre>
 */
public final class LoggingContext implements AutoCloseable {

    private final Map<String, String> previousContext;

    private LoggingContext(Map<String, String> previousContext) {
        this.previousContext = previousContext;
    }

    /**
     * Replace or add the given MDC entries for the lifetime of the returned scope.
     * Previous MDC contents are restored when the scope closes.
     */
    public static LoggingContext withValues(Map<String, String> entries) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (previous == null) {
            previous = Collections.emptyMap();
        }

        entries.forEach((key, value) -> {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        });

        return new LoggingContext(previous);
    }

    /**
     * Replace or add a single MDC key for the lifetime of the returned scope.
     * Previous MDC contents are restored when the scope closes.
     */
    public static LoggingContext withValue(String key, String value) {
        return withValues(Map.of(key, value));
    }

    @Override
    public void close() {
        if (previousContext.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(previousContext);
        }
    }
}
