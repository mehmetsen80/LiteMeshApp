package org.lite.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.ErrorCode;
import org.lite.gateway.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TeamOperationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTeamOperationException(TeamOperationException ex) {
        log.error("Team operation error: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
            .badRequest()
            .body(ErrorResponse.fromErrorCode(
                ErrorCode.TEAM_OPERATION_ERROR,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
            )));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Validation error: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
            .badRequest()
            .body(ErrorResponse.fromErrorCode(
                ErrorCode.VALIDATION_ERROR,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
            )));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.fromErrorCode(
                ErrorCode.INTERNAL_ERROR,
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            )));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnauthorizedException(UnauthorizedException ex) {
        log.error("Authorization error: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.fromErrorCode(
                ErrorCode.UNAUTHORIZED,
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value()
            )));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage(), ex);
        
        // Determine the appropriate error code based on the error message
        ErrorCode errorCode;
        String message = ex.getMessage().toLowerCase();
        
        if (message.contains("team")) {
            errorCode = ErrorCode.TEAM_NOT_FOUND;
        } else if (message.contains("route")) {
            errorCode = ErrorCode.ROUTE_NOT_FOUND;
        } else if (message.contains("user")) {
            errorCode = ErrorCode.USER_NOT_FOUND;
        } else if (message.contains("organization")) {
            errorCode = ErrorCode.ORGANIZATION_NOT_FOUND;
        } else if (message.contains("member")) {
            errorCode = ErrorCode.MEMBER_NOT_FOUND;
        } else {
            // If we can't determine the specific resource type, use a generic system error
            errorCode = ErrorCode.VALIDATION_ERROR;
        }

        return Mono.just(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.fromErrorCode(
                errorCode,
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value()
            )));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        log.error("Invalid credentials: {}", ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.fromErrorCode(
                ex.getErrorCode(),
                HttpStatus.UNAUTHORIZED.value()
            )));
    }

    @ExceptionHandler(DuplicateUserException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicateUserException(DuplicateUserException ex) {
        log.error("Duplicate user: {}", ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse.fromErrorCode(
                ErrorCode.USER_ALREADY_EXISTS,
                ex.getMessage(),
                HttpStatus.CONFLICT.value()
            )));
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(ValidationException ex) {
        log.error("Validation error: {}", ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.fromErrorCode(
                ErrorCode.VALIDATION_ERROR,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
            )));
    }
}
