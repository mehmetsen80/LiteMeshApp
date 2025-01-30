package org.lite.gateway.repository;

import org.lite.gateway.entity.ApiRoute;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiRouteRepository extends ReactiveMongoRepository<ApiRoute, String> {
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

    Mono<Boolean> existsByRouteIdentifier(String routeIdentifier);

    @Query("{ $and: [ " +
           "  { $or: [ " +
           "    { 'routeIdentifier': { $regex: ?0, $options: 'i' }}, " +
           "    { 'path': { $regex: ?0, $options: 'i' }}, " +
           "    { 'uri': { $regex: ?0, $options: 'i' }} " +
           "  ]}, " +
           "  { $or: [ " +
           "    { 'method': ?1 }, " +
           "    { 'method': '' }, " +
           "    { 'method': null } " +
           "  ]}, " +
           "  { 'healthCheck.enabled': ?2 } " +
           "]}")
    Flux<ApiRoute> searchRoutes(String searchTerm, String method, Boolean healthCheckEnabled);
} 