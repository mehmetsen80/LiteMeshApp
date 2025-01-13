package org.lite.gateway.repository;

import org.lite.gateway.entity.RouteVersionMetadata;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface RouteVersionMetadataRepository extends ReactiveMongoRepository<RouteVersionMetadata, String> {
    Flux<RouteVersionMetadata> findByRouteIdentifierOrderByVersionDesc(String routeIdentifier);
} 