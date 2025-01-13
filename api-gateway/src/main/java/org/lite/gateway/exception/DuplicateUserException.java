package org.lite.gateway.exception;

public class DuplicateUserException extends AuthException {
    public DuplicateUserException(String message) {
        super(message);
    }
} 