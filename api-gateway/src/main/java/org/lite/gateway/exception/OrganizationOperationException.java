package org.lite.gateway.exception;

public class OrganizationOperationException extends RuntimeException {
    public OrganizationOperationException(String message) {
        super(message);
    }

    public OrganizationOperationException(String message, Throwable cause) {
        super(message, cause);
    }
} 