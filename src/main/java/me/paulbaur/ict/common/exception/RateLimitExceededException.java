package me.paulbaur.ict.common.exception;

/**
 * Exception thrown when API rate limit is exceeded.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
