package org.lite.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import io.jsonwebtoken.JwtException;
import java.net.ConnectException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.lite.gateway.dto.ErrorResponse;
import org.lite.gateway.dto.DetailedErrorResponse;
import org.lite.gateway.dto.ValidationErrorResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Error categories for better client handling
    public enum ErrorCategory {
        AUTHENTICATION,    // Auth related errors
        VALIDATION,        // Input validation errors
        BUSINESS_LOGIC,    // Business rule violations
        INFRASTRUCTURE,    // System/infrastructure issues
        EXTERNAL_SERVICE,  // External service errors
        UNKNOWN           // Unexpected errors
    }

    @ExceptionHandler(DuplicateUserException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicateUser(DuplicateUserException ex) {
        String trackingId = generateTrackingId();
        log.error("DuplicateUser error [{}]: {}", trackingId, ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                "DUPLICATE_USER",
                ex.getMessage(),
                HttpStatus.CONFLICT.value()
            )));
    }

    @ExceptionHandler(DataAccessException.class)
    public Mono<ResponseEntity<DetailedErrorResponse>> handleDataAccess(DataAccessException ex) {
        String trackingId = generateTrackingId();
        log.error("Database error [{}]: {}", trackingId, ex.getMessage(), ex);
        
        Map<String, Object> details = new HashMap<>();
        details.put("operation", ex.getClass().getSimpleName());
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new DetailedErrorResponse(
                "DATABASE_ERROR",
                "Database operation failed",
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                trackingId,
                ex.getClass().getSimpleName(),
                ErrorCategory.INFRASTRUCTURE,
                details
            )));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return Mono.just(ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(
                "INVALID_CREDENTIALS",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value()
            )));
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(ValidationException ex) {
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                "VALIDATION_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
            )));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ValidationErrorResponse>> handleValidationErrors(WebExchangeBindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                errors
            )));
    }

    @ExceptionHandler(AuthException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAuthException(AuthException ex) {
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                "AUTH_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
            )));
    }

    @ExceptionHandler(JwtException.class)
    public Mono<ResponseEntity<DetailedErrorResponse>> handleJwt(JwtException ex) {
        String trackingId = generateTrackingId();
        log.error("JWT error [{}]: {}", trackingId, ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorType", ex.getClass().getSimpleName());
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new DetailedErrorResponse(
                "JWT_ERROR",
                "Invalid or expired token",
                HttpStatus.UNAUTHORIZED.value(),
                trackingId,
                ex.getClass().getSimpleName(),
                ErrorCategory.AUTHENTICATION,
                details
            )));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<DetailedErrorResponse>> handleWebClient(WebClientResponseException ex) {
        String trackingId = generateTrackingId();
        log.error("External service error [{}]: {} - {}", trackingId, ex.getStatusCode(), ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("statusCode", ex.getStatusCode().value());
        details.put("response", ex.getResponseBodyAsString());
        
        return Mono.just(ResponseEntity
            .status(ex.getStatusCode())
            .body(new DetailedErrorResponse(
                "EXTERNAL_SERVICE_ERROR",
                "External service request failed",
                ex.getStatusCode().value(),
                trackingId,
                ex.getClass().getSimpleName(),
                ErrorCategory.EXTERNAL_SERVICE,
                details
            )));
    }

    @ExceptionHandler(ConnectException.class)
    public Mono<ResponseEntity<DetailedErrorResponse>> handleConnect(ConnectException ex) {
        String trackingId = generateTrackingId();
        log.error("Connection error [{}]: {}", trackingId, ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("host", ex.getMessage().replaceAll(".*: ", ""));
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new DetailedErrorResponse(
                "CONNECTION_ERROR",
                "Failed to connect to service",
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                trackingId,
                ex.getClass().getSimpleName(),
                ErrorCategory.INFRASTRUCTURE,
                details
            )));
    }

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<DetailedErrorResponse>> handleRuntime(RuntimeException ex) {
        String trackingId = generateTrackingId();
        log.error("Unexpected error [{}]. Type: {}, Message: {}",
            trackingId, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        Map<String, Object> details = new HashMap<>();
        details.put("errorType", ex.getClass().getSimpleName());
        details.put("timestamp", System.currentTimeMillis());

        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new DetailedErrorResponse(
                "INTERNAL_ERROR",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                trackingId,
                ex.getClass().getSimpleName(),
                ErrorCategory.UNKNOWN,
                details
            )));
    }

    private String generateTrackingId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
