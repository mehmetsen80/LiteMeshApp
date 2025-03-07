package org.lite.gateway.exception;

import org.lite.gateway.dto.ErrorCode;

public class ResourceNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public ResourceNotFoundException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
} 