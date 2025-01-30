package org.lite.gateway.exception;

public class TeamOperationException extends RuntimeException {
    public TeamOperationException(String message) {
        super(message);
    }

    public TeamOperationException(String message, Throwable cause) {
        super(message, cause);
    }
} 