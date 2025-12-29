package me.paulbaur.ict.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import me.paulbaur.ict.common.exception.CircuitBreakerOpenException;
import me.paulbaur.ict.common.exception.NotFoundException;
import me.paulbaur.ict.common.exception.RateLimitExceededException;
import me.paulbaur.ict.common.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        String message = ex.getMessage();
        log.warn(
                "Resource not found",
                kv("errorCode", "NOT_FOUND"),
                kv("status", HttpStatus.NOT_FOUND.value()),
                kv("message", message),
                kv("path", request.getRequestURI())
        );
        return buildError(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
        String message = ex.getMessage();
        log.warn(
                "Rate limit exceeded",
                kv("errorCode", "RATE_LIMIT_EXCEEDED"),
                kv("status", HttpStatus.TOO_MANY_REQUESTS.value()),
                kv("message", message),
                kv("path", request.getRequestURI())
        );
        return buildError(message, "RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(CircuitBreakerOpenException ex, HttpServletRequest request) {
        String message = "Service temporarily unavailable";
        log.error(
                "Circuit breaker open",
                kv("errorCode", "SERVICE_UNAVAILABLE"),
                kv("status", HttpStatus.SERVICE_UNAVAILABLE.value()),
                kv("message", ex.getMessage()),
                kv("path", request.getRequestURI())
        );
        return buildError(message, "SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildValidationError(ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .findFirst()
                .orElse("Request validation failed");
        return buildValidationError(message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return buildValidationError(message.isBlank() ? "Request validation failed" : message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = "%s parameter is required".formatted(ex.getParameterName());
        return buildValidationError(message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildValidationError("Malformed JSON request", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error(
                "Unhandled exception",
                kv("status", HttpStatus.INTERNAL_SERVER_ERROR.value()),
                kv("path", request.getRequestURI()),
                kv("exception", ex.getClass().getSimpleName()),
                ex
        );
        return buildError("Unexpected error", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildValidationError(String message, HttpServletRequest request) {
        log.warn(
                "Request validation failed",
                kv("errorCode", "VALIDATION_ERROR"),
                kv("status", HttpStatus.BAD_REQUEST.value()),
                kv("message", message),
                kv("path", request.getRequestURI())
        );
        return buildError(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    private static ResponseEntity<ErrorResponse> buildError(String message, String code, HttpStatus status) {
        ErrorResponse error = new ErrorResponse(message, code, Instant.now());
        return ResponseEntity.status(status).body(error);
    }

    private static String formatFieldError(FieldError fieldError) {
        return "%s %s".formatted(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
