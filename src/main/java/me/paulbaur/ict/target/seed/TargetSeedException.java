package me.paulbaur.ict.target.seed;

/**
 * Signals failures while loading or validating target seed definitions.
 */
public class TargetSeedException extends RuntimeException {
    public TargetSeedException(String message) {
        super(message);
    }

    public TargetSeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
