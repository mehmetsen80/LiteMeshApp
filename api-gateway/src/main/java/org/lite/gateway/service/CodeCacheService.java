package org.lite.gateway.service;

import reactor.core.publisher.Mono;

public interface CodeCacheService {
    Mono<Boolean> isCodeUsed(String code);
    Mono<Boolean> markCodeAsUsed(String code);
} 