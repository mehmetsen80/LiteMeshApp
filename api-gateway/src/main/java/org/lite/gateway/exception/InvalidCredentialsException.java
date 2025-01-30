package org.lite.gateway.exception;

import org.lite.gateway.dto.ErrorCode;

public class InvalidCredentialsException extends RuntimeException {
    private final ErrorCode errorCode;

    public InvalidCredentialsException() {
        this(ErrorCode.USER_INVALID_CREDENTIALS);
    }

    public InvalidCredentialsException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
} 