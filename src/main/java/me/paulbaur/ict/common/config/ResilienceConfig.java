package me.paulbaur.ict.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j retry and circuit breaker patterns.
 */
@Slf4j
@Configuration
public class ResilienceConfig {

    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public Retry elasticsearchRetry(RetryRegistry retryRegistry) {
        Retry retry = retryRegistry.retry("elasticsearch");

        retry.getEventPublisher()
            .onRetry(event -> log.warn("Elasticsearch operation retry attempt {} due to: {}",
                event.getNumberOfRetryAttempts(),
                event.getLastThrowable().getMessage()))
            .onSuccess(event -> {
                if (event.getNumberOfRetryAttempts() > 0) {
                    log.info("Elasticsearch operation succeeded after {} retries",
                        event.getNumberOfRetryAttempts());
                }
            });

        return retry;
    }

    @Bean
    public CircuitBreaker elasticsearchCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("elasticsearch");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.warn("Elasticsearch circuit breaker state transition: {} -> {}",
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState()))
            .onFailureRateExceeded(event -> log.error("Elasticsearch circuit breaker failure rate exceeded: {}%",
                event.getFailureRate()))
            .onCallNotPermitted(event -> log.error("Elasticsearch circuit breaker is OPEN - calls not permitted"));

        return circuitBreaker;
    }

    @Bean
    public Retry tcpProbeRetry(RetryRegistry retryRegistry) {
        Retry retry = retryRegistry.retry("tcpProbe");

        retry.getEventPublisher()
            .onRetry(event -> log.debug("TCP probe retry attempt {} for: {}",
                event.getNumberOfRetryAttempts(),
                event.getLastThrowable().getMessage()));

        return retry;
    }
}
