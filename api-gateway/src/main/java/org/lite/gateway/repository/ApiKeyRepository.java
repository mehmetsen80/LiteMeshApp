package org.lite.gateway.repository;

import org.lite.gateway.entity.ApiKey;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ApiKeyRepository extends ReactiveMongoRepository<ApiKey, String> {
    Mono<ApiKey> findByKey(String key);
    Mono<ApiKey> findByTeamId(String teamId);
    Mono<Boolean> existsByKey(String key);
    Flux<ApiKey> findByEnabledTrue();
    Flux<ApiKey> findByCreatedBy(String username);
} 