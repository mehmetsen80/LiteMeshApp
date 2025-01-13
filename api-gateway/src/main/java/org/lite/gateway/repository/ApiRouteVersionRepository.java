package org.lite.gateway.repository;

import org.lite.gateway.entity.ApiRouteVersion;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiRouteVersionRepository extends ReactiveMongoRepository<ApiRouteVersion, String> {
    Flux<ApiRouteVersion> findByRouteIdentifierOrderByVersionDesc(String routeIdentifier);
    Mono<ApiRouteVersion> findByRouteIdentifierAndVersion(String routeIdentifier, Integer version);
    Mono<ApiRouteVersion> findFirstByRouteIdentifierOrderByVersionDesc(String routeIdentifier);
} 