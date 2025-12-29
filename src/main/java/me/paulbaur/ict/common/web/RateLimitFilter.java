package me.paulbaur.ict.common.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using token bucket algorithm (Bucket4j).
 * Limits API requests per client IP address.
 */
@Slf4j
@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final boolean enabled;
    private final long requestsPerMinute;
    private final long burstCapacity;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${ict.ratelimit.enabled:true}") boolean enabled,
            @Value("${ict.ratelimit.requests-per-minute:100}") long requestsPerMinute,
            @Value("${ict.ratelimit.burst-capacity:20}") long burstCapacity) {
        this.enabled = enabled;
        this.requestsPerMinute = requestsPerMinute;
        this.burstCapacity = burstCapacity;
        log.info("RateLimitFilter initialized - enabled: {}, requests/min: {}, burst: {}",
                enabled, requestsPerMinute, burstCapacity);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Only apply rate limiting to /api/** endpoints
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpRequest);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
            log.warn("Rate limit exceeded for client IP: {} on path: {}", clientIp, path);
        }
    }

    private Bucket createBucket(String key) {
        // Refill bucket at steady rate of requestsPerMinute tokens per minute
        Refill refill = Refill.intervally(requestsPerMinute, Duration.ofMinutes(1));

        // Burst capacity allows brief spikes
        Bandwidth limit = Bandwidth.classic(burstCapacity, refill);

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for X-Forwarded-For header (proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }
}
