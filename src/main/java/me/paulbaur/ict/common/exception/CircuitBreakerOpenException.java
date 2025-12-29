package me.paulbaur.ict.common.exception;

/**
 * Exception thrown when a circuit breaker is open and rejecting calls.
 */
public class CircuitBreakerOpenException extends RuntimeException {

    public CircuitBreakerOpenException(String message) {
        super(message);
    }

    public CircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
