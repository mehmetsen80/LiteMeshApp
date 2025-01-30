package org.lite.gateway.service;

import org.lite.gateway.dto.RouteExistenceRequest;
import org.lite.gateway.dto.RouteExistenceResponse;
import org.lite.gateway.dto.VersionComparisonResult;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.RouteVersionMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiRouteService {
    
    // Basic CRUD operations
    Flux<ApiRoute> getAllRoutes();
    Mono<ApiRoute> getRouteById(String id);
    Mono<ApiRoute> getRouteByIdentifier(String routeIdentifier);
    Mono<ApiRoute> createRoute(ApiRoute route);
    Mono<ApiRoute> updateRoute(ApiRoute route);
    Mono<Void> deleteRoute(String id);
    
    // Search operations
    Flux<ApiRoute> searchRoutes(String searchTerm, String method, Boolean healthCheckEnabled);
    
    // Version management
    Flux<ApiRoute> getAllVersions(String routeIdentifier);
    Mono<ApiRoute> getSpecificVersion(String routeIdentifier, Integer version);
    Mono<VersionComparisonResult> compareVersions(String routeIdentifier, Integer version1, Integer version2);
    Mono<ApiRoute> rollbackToVersion(String routeIdentifier, Integer version);
    
    // Version metadata
    Flux<RouteVersionMetadata> getVersionMetadata(String routeIdentifier);
    
    // Route existence check
    Mono<RouteExistenceResponse> checkRouteExistence(RouteExistenceRequest request);
}
