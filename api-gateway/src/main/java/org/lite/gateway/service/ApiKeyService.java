package org.lite.gateway.service;

import org.lite.gateway.entity.ApiKey;
import reactor.core.publisher.Mono;

public interface ApiKeyService {
    Mono<ApiKey> validateApiKey(String apiKey);
    Mono<ApiKey> createApiKey(String name, String teamId, String createdBy, Long expiresInDays);
    Mono<Void> revokeApiKey(String apiKeyId);
} 