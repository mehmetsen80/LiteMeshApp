package org.lite.gateway.service;

import org.lite.gateway.dto.AuthResponse;
import reactor.core.publisher.Mono;

public interface KeycloakService {
    Mono<AuthResponse> handleCallback(String code);
} 