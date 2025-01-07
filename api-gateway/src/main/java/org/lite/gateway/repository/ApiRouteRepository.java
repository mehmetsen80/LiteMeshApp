package org.lite.gateway.repository;

import org.lite.gateway.entity.ApiRoute;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ApiRouteRepository extends ReactiveCrudRepository<ApiRoute, String> {
    @Query("{ 'healthCheck.enabled': true }")
    Flux<ApiRoute> findAllWithHealthCheckEnabled();
    
    @Query("{ 'routeIdentifier': ?0, 'healthCheck.enabled': true }")
    Mono<ApiRoute> findHealthCheckConfigByRouteIdentifier(String routeIdentifier);
    
    @Query(value = "{ 'healthCheck.enabled': true }", fields = "{ 'routeIdentifier': 1, 'healthCheck': 1 }")
    Flux<ApiRoute> findAllHealthCheckConfigs();

    @Query("{ 'routeIdentifier': ?0 }")
    Mono<ApiRoute> findByRouteIdentifier(String routeIdentifier);

    @Query(value = "{ 'id': ?0 }")
    @NonNull
    Mono<ApiRoute> findById(@NonNull String id);
} 