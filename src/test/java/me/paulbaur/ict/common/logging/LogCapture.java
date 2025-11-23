package me.paulbaur.ict.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Test helper to capture Logback events and inspect structured arguments.
 */
public final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender;
    private final Level previousLevel;

    private LogCapture(Logger logger, Level level) {
        this.logger = logger;
        this.previousLevel = logger.getLevel();

        if (level != null) {
            logger.setLevel(level);
        }

        this.appender = new ListAppender<>();
        this.appender.start();
        this.logger.addAppender(appender);
    }

    public static LogCapture capture(Class<?> type, Level level) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        return new LogCapture(logger, level);
    }

    public List<ILoggingEvent> events() {
        return appender.list;
    }

    public Optional<ILoggingEvent> firstMatching(Predicate<ILoggingEvent> predicate) {
        return appender.list.stream().filter(predicate).findFirst();
    }

    public static Map<String, Object> structuredArguments(ILoggingEvent event) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object[] args = event.getArgumentArray();
        if (args == null) {
            return result;
        }

        for (Object arg : args) {
            if (arg instanceof Map.Entry<?, ?> entry) {
                result.put(String.valueOf(entry.getKey()), coerceValue(entry.getValue()));
            } else if (arg != null) {
                String rendered = arg.toString();
                int separator = rendered.indexOf('=');
                if (separator > 0) {
                    String key = rendered.substring(0, separator);
                    String value = rendered.substring(separator + 1);
                    result.put(key, coerceValue(value));
                }
            }
        }
        return result;
    }

    private static Object coerceValue(Object value) {
        if (!(value instanceof String str)) {
            return value;
        }

        if (str.matches("-?\\d+")) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                // Fall back to long if the integer range is exceeded
            }
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return str;
            }
        }

        return str;
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
    }
}
