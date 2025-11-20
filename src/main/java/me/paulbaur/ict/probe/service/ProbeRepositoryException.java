package me.paulbaur.ict.probe.service;

/**
 * A runtime exception representing failures inside the probe repository layer.
 *
 * This unchecked exception wraps lower-level exceptions (for example, Elasticsearch
 * client failures) so callers can decide whether to handle or propagate repository
 * errors.
 */
public class ProbeRepositoryException extends RuntimeException {
    public ProbeRepositoryException(String message) {
        super(message);
    }

    public ProbeRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}

