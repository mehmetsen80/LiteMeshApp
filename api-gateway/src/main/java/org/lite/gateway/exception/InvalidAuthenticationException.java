package org.lite.gateway.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidAuthenticationException extends AuthenticationException {
    public InvalidAuthenticationException(String msg) {
        super(msg);
    }
} 