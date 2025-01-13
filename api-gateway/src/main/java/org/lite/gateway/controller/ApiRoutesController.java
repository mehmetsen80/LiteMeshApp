package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.RouteVersionMetadata;
import org.lite.gateway.exception.DuplicateRouteException;
import org.lite.gateway.dto.ErrorResponse;
import org.lite.gateway.dto.RouteExistenceResponse;
import org.lite.gateway.service.ApiRoutesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.lite.gateway.dto.RouteExistenceResponse.ExistenceDetail;
import org.lite.gateway.dto.RouteExistenceRequest;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
public class ApiRoutesController {

    private final ApiRoutesService apiRoutesService;

    @GetMapping
    public Flux<ApiRoute> getAllRoutes() {
        log.info("Fetching all API routes");
        return apiRoutesService.getAllRoutes();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiRoute>> getRoute(@PathVariable String id) {
        log.info("Fetching route with id: {}", id);
        return apiRoutesService.getRouteById(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/identifier/{routeIdentifier}")
    public Mono<ResponseEntity<ApiRoute>> getRouteByIdentifier(@PathVariable String routeIdentifier) {
        log.info("Fetching route with identifier: {}", routeIdentifier);
        return apiRoutesService.getRouteByIdentifier(routeIdentifier)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createRoute(@Valid @RequestBody ApiRoute route) {
        log.info("Creating new route with identifier: {}", route.getRouteIdentifier());
        return apiRoutesService.createRoute(route)
            .<ResponseEntity<Object>>map(savedRoute -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedRoute))
            .onErrorResume(DuplicateRouteException.class, e -> 
                Mono.just(ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(
                        "DUPLICATE_ROUTE",
                        e.getMessage(),
                        HttpStatus.CONFLICT.value()
                    ))));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiRoute>> updateRoute(
            @PathVariable String id,
            @Valid @RequestBody ApiRoute route) {
        log.info("Updating route with id: {}", id);
        route.setId(id); // Ensure ID matches path variable
        return apiRoutesService.updateRoute(route)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteRoute(@PathVariable String id) {
        log.info("Deleting route with id: {}", id);
        return apiRoutesService.deleteRoute(id)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public Flux<ApiRoute> searchRoutes(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Boolean healthCheckEnabled) {
        log.info("Searching routes with term: {}, method: {}, healthCheck: {}", 
            searchTerm, method, healthCheckEnabled);
        return apiRoutesService.searchRoutes(searchTerm, method, healthCheckEnabled);
    }

    @GetMapping("/identifier/{routeIdentifier}/versions")
    public Flux<ApiRoute> getAllVersions(@PathVariable String routeIdentifier) {
        log.info("Fetching all versions for route: {}", routeIdentifier);
        return apiRoutesService.getAllVersions(routeIdentifier);
    }

    @GetMapping("/identifier/{routeIdentifier}/versions/{version}")
    public Mono<ResponseEntity<Object>> getSpecificVersion(
            @PathVariable String routeIdentifier,
            @PathVariable Integer version) {
        log.info("Fetching version {} of route: {}", version, routeIdentifier);
        return apiRoutesService.getSpecificVersion(routeIdentifier, version)
            .<ResponseEntity<Object>>map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                    "VERSION_NOT_FOUND",
                    String.format("Version %d of route '%s' does not exist", 
                        version, routeIdentifier),
                    HttpStatus.NOT_FOUND.value()
                )));
    }

    @GetMapping("/identifier/{routeIdentifier}/compare")
    public Mono<ResponseEntity<Object>> compareVersions(
            @PathVariable String routeIdentifier,
            @RequestParam Integer version1,
            @RequestParam Integer version2) {
        log.info("Comparing versions {} and {} of route: {}", version1, version2, routeIdentifier);
        return apiRoutesService.compareVersions(routeIdentifier, version1, version2)
            .<ResponseEntity<Object>>map(ResponseEntity::ok)
            .onErrorResume(e -> 
                Mono.just(ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(
                        "VERSION_COMPARISON_ERROR",
                        String.format("Cannot compare versions %d and %d of route '%s'. " +
                            "Make sure both versions exist and the route identifier is correct.", 
                            version1, version2, routeIdentifier),
                        HttpStatus.NOT_FOUND.value()
                    ))))
            .defaultIfEmpty(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                    "ROUTE_NOT_FOUND",
                    String.format("Route '%s' does not exist", routeIdentifier),
                    HttpStatus.NOT_FOUND.value()
                )));
    }

    @PostMapping("/identifier/{routeIdentifier}/rollback/{version}")
    public Mono<ResponseEntity<ApiRoute>> rollbackToVersion(
            @PathVariable String routeIdentifier,
            @PathVariable Integer version) {
        log.info("Rolling back route {} to version {}", routeIdentifier, version);
        return apiRoutesService.rollbackToVersion(routeIdentifier, version)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/identifier/{routeIdentifier}/metadata")
    public Flux<RouteVersionMetadata> getVersionMetadata(@PathVariable String routeIdentifier) {
        log.info("Fetching version metadata for route: {}", routeIdentifier);
        return apiRoutesService.getVersionMetadata(routeIdentifier);
    }

    @GetMapping("/check")
    public Mono<ResponseEntity<RouteExistenceResponse>> checkRouteExistence(
            @Valid RouteExistenceRequest request) {
        log.info("Checking existence for id: {} or identifier: {}", 
            request.getId(), request.getRouteIdentifier());
        
        if (request.getId() == null && request.getRouteIdentifier() == null) {
            return Mono.just(ResponseEntity
                .badRequest()
                .body(RouteExistenceResponse.builder()
                    .exists(false)
                    .message("Either id or routeIdentifier must be provided")
                    .detail(ExistenceDetail.builder()
                        .validationMessage("Missing required parameters")
                        .build())
                    .build()));
        }
        
        return apiRoutesService.checkRouteExistence(request)
            .map(ResponseEntity::ok);
    }
} 