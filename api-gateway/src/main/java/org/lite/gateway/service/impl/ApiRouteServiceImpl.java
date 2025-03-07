package org.lite.gateway.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.lite.gateway.dto.ErrorCode;
import org.lite.gateway.dto.RouteChangeDetails;
import org.lite.gateway.dto.RouteExistenceRequest;
import org.lite.gateway.dto.RouteExistenceResponse;
import org.lite.gateway.dto.VersionComparisonResult;
import org.lite.gateway.entity.*;
import org.lite.gateway.enums.UserRole;
import org.lite.gateway.exception.DuplicateRouteException;
import org.lite.gateway.repository.ApiRouteRepository;
import org.lite.gateway.repository.ApiRouteVersionRepository;
import org.lite.gateway.repository.RouteVersionMetadataRepository;
import org.lite.gateway.service.ApiRouteService;
import org.lite.gateway.service.UserContextService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.lite.gateway.repository.TeamRouteRepository;
import org.lite.gateway.repository.UserRepository;
import org.lite.gateway.repository.TeamMemberRepository;
import org.lite.gateway.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiRouteServiceImpl implements ApiRouteService {
    private final ApiRouteRepository apiRouteRepository;
    private final ObjectMapper objectMapper;
    private final RouteVersionMetadataRepository metadataRepository;
    private final UserContextService userContextService;
    private final ApiRouteVersionRepository apiRouteVersionRepository;
    private final TeamRouteRepository teamRouteRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    public Flux<ApiRoute> getAllRoutes() {
        return apiRouteRepository.findAll()
                .doOnComplete(() -> log.info("Finished fetching all routes"))
                .doOnError(error -> log.error("Error fetching routes: {}", error.getMessage()));
    }

    public Mono<ApiRoute> getRouteById(String id) {
        return apiRouteRepository.findById(id)
                .doOnSuccess(route -> {
                    if (route != null) {
                        log.info("Found route: {}", route.getRouteIdentifier());
                    } else {
                        log.info("No route found with id: {}", id);
                    }
                })
                .doOnError(error -> log.error("Error fetching route {}: {}", id, error.getMessage()));
    }

    public Mono<ApiRoute> getRouteByIdentifier(String routeIdentifier) {
        return apiRouteRepository.findByRouteIdentifier(routeIdentifier)
                .doOnSuccess(route -> {
                    if (route != null) {
                        log.info("Found route with identifier: {}", routeIdentifier);
                    } else {
                        log.info("No route found with identifier: {}", routeIdentifier);
                    }
                })
                .doOnError(error ->
                        log.error("Error fetching route with identifier {}: {}",
                                routeIdentifier, error.getMessage()));
    }

    public Mono<ApiRoute> createRoute(ApiRoute route) {
        return Mono.just(route)
                .flatMap(this::checkDuplicates)
                .flatMap(apiRouteRepository::save)
                .flatMap(savedRoute -> {
                    return saveVersionIfNotExists(savedRoute)
                            .then(Mono.just(savedRoute));
                })
                .flatMap(savedRoute -> {
                    Mono<RouteChangeDetails> detailsMono = generateChangeDetails(null, savedRoute, "Initial route creation");
                    return detailsMono.flatMap(details ->
                            saveVersionMetadata(savedRoute, details, RouteVersionMetadata.ChangeType.CREATE)
                    );
                })
                .doOnSuccess(savedRoute ->
                        log.info("Successfully created route: {}", savedRoute.getRouteIdentifier()));
    }

    private Mono<ApiRoute> checkDuplicates(ApiRoute route) {
        // Only check for duplicate identifiers during creation (when id is null)
        if (route.getId() == null) {
            return apiRouteRepository.existsByRouteIdentifier(route.getRouteIdentifier())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new DuplicateRouteException(
                            "Route with identifier " + route.getRouteIdentifier() + " already exists"));
                    }
                    return Mono.just(route);
                });
        }
        
        // During update, check both id and identifier don't conflict with other routes
        return apiRouteRepository.findByRouteIdentifier(route.getRouteIdentifier())
            .flatMap(existingRoute -> {
                if (!existingRoute.getId().equals(route.getId())) {
                    return Mono.error(new DuplicateRouteException(
                        "Route with identifier " + route.getRouteIdentifier() + " already exists"));
                }
                return Mono.just(route);
            })
            .switchIfEmpty(Mono.just(route));
    }

    public Mono<ApiRoute> updateRoute(ApiRoute route) {
        return apiRouteRepository.findByRouteIdentifier(route.getRouteIdentifier())
                .flatMap(existingRoute -> {
                    // Preserve immutable fields
                    route.setId(existingRoute.getId());
                    route.setRouteIdentifier(existingRoute.getRouteIdentifier());
                    route.setUri(existingRoute.getUri());
                    route.setPath(existingRoute.getPath());
                   
                    // Handle mutable fields
                    if (route.getMethod() == null || route.getMethod().isEmpty()) {
                        route.setMethod(existingRoute.getMethod());
                    }
                    if (route.getScope() == null) {
                        route.setScope(existingRoute.getScope());
                    }
                    if (route.getMaxCallsPerDay() == null) {
                        route.setMaxCallsPerDay(existingRoute.getMaxCallsPerDay());
                    }

                    // Handle filters
                    if (route.getFilters() != null) {
                        route.getFilters().forEach(filter -> {
                            // Handle CircuitBreaker special cases
                            if ("CircuitBreaker".equals(filter.getName()) && filter.getArgs() != null) {
                                existingRoute.getFilters().stream()
                                    .filter(ef -> "CircuitBreaker".equals(ef.getName()))
                                    .findFirst()
                                    .ifPresent(existingFilter -> {
                                        // Preserve recordFailurePredicate and HttpResponsePredicate
                                        if (existingFilter.getArgs().containsKey("recordFailurePredicate")) {
                                            filter.getArgs().put("recordFailurePredicate", 
                                                existingFilter.getArgs().get("recordFailurePredicate"));
                                        }
                                    });
                            }
                            
                            // Validate and convert numeric values for all filters
                            if (filter.getArgs() != null) {
                                validateAndConvertFilterArgs(filter);
                            }
                        });
                    } else {
                        route.setFilters(existingRoute.getFilters());
                    }
                   
                    route.setVersion(existingRoute.getVersion() + 1);
                    route.setCreatedAt(existingRoute.getCreatedAt());
                    route.setUpdatedAt(System.currentTimeMillis());

                    // Handle health check
                    if (route.getHealthCheck() != null) {
                        sanitizeHealthCheckConfig(route.getHealthCheck());
                    } else {
                        route.setHealthCheck(existingRoute.getHealthCheck());
                    }

                    return saveVersionIfNotExists(existingRoute)
                            .then(saveVersionIfNotExists(route))
                            .then(apiRouteRepository.save(route))
                            .flatMap(savedRoute -> {
                                Mono<RouteChangeDetails> detailsMono = generateChangeDetails(existingRoute, savedRoute, "Route configuration updated");
                                return detailsMono.flatMap(details ->
                                        saveVersionMetadata(savedRoute, details, RouteVersionMetadata.ChangeType.UPDATE)
                                );
                            });
                })
                .doOnSuccess(updatedRoute ->
                        log.info("Successfully updated route: {}", updatedRoute.getRouteIdentifier()))
                .doOnError(error ->
                        log.error("Error updating route: {}", error.getMessage()));
    }

    private void validateAndConvertFilterArgs(FilterConfig filter) {
        switch (filter.getName()) {
            case "RedisRateLimiter":
                ensureNumericValue(filter.getArgs(), "replenishRate");
                ensureNumericValue(filter.getArgs(), "burstCapacity");
                ensureNumericValue(filter.getArgs(), "requestedTokens");
                break;
            case "TimeLimiter":
                ensureNumericValue(filter.getArgs(), "timeoutDuration");
                ensureBooleanValue(filter.getArgs(), "cancelRunningFuture");
                break;
            case "CircuitBreaker":
                ensureNumericValue(filter.getArgs(), "slidingWindowSize");
                ensureNumericValue(filter.getArgs(), "failureRateThreshold");
                ensureNumericValue(filter.getArgs(), "permittedNumberOfCallsInHalfOpenState");
                ensureBooleanValue(filter.getArgs(), "automaticTransitionFromOpenToHalfOpenEnabled");
                break;
            case "Retry":
                ensureNumericValue(filter.getArgs(), "maxAttempts");
                break;
        }
    }

    private void ensureNumericValue(Map<String, String> args, String key) {
        if (args.containsKey(key)) {
            try {
                Integer.parseInt(args.get(key));
            } catch (NumberFormatException e) {
                log.warn("Invalid numeric value for {}: {}", key, args.get(key));
            }
        }
    }

    private void ensureBooleanValue(Map<String, String> args, String key) {
        if (args.containsKey(key)) {
            String value = args.get(key).toLowerCase();
            if (!value.equals("true") && !value.equals("false")) {
                args.put(key, "false");
            }
        }
    }

    private Mono<ApiRouteVersion> saveVersionIfNotExists(ApiRoute route) {
        return apiRouteVersionRepository
                .findByRouteIdentifierAndVersion(route.getRouteIdentifier(), route.getVersion())
                .hasElement()
                .flatMap(versionExists -> {
                    if (!versionExists) {
                        ApiRouteVersion version = ApiRouteVersion.builder()
                                .routeId(route.getId())
                                .routeIdentifier(route.getRouteIdentifier())
                                .version(route.getVersion())
                                .routeData(route)
                                .createdAt(System.currentTimeMillis())
                                .build();
                        return apiRouteVersionRepository.save(version);
                    }
                    return Mono.empty();
                });
    }

    public Mono<Void> deleteRoute(String routeIdentifier, String teamId, String username) {
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + username, ErrorCode.USER_NOT_FOUND)))
            .flatMap(user -> teamMemberRepository.findByTeamIdAndUserIdAndRole(teamId, user.getId(), UserRole.valueOf("ADMIN"))
                .switchIfEmpty(Mono.error(new AccessDeniedException(
                    "You don't have administrator rights in this team to delete routes"
                )))
                .flatMap(teamMember -> apiRouteRepository.findByRouteIdentifier(routeIdentifier)
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Route not found with identifier: " + routeIdentifier, 
                        ErrorCode.ROUTE_NOT_FOUND
                    )))
                    .flatMap(route -> teamRouteRepository.findByRouteId(route.getId())
                        .collectList()
                        .flatMap(teamRoutes -> {
                            if (!teamRoutes.isEmpty()) {
                                String errorMessage = String.format(
                                    "Cannot delete route. It is currently assigned to %d team(s). " +
                                    "Please remove all team assignments before deletion.", 
                                    teamRoutes.size()
                                );
                                return Mono.error(new IllegalStateException(errorMessage));
                            }
                            log.info("Route with identifier: {} (id: {}) is being deleted by user: {} (id: {}) in team: {}", 
                                routeIdentifier, route.getId(), username, user.getId(), teamId);
                            return apiRouteRepository.delete(route);
                        })
                    )
                )
            );
    }

    public Flux<ApiRoute> searchRoutes(String searchTerm, String method, Boolean healthCheckEnabled) {
        searchTerm = searchTerm != null ? searchTerm : "";
        method = method != null ? method.toUpperCase() : "";
        healthCheckEnabled = healthCheckEnabled != null ? healthCheckEnabled : true;

        String finalSearchTerm = searchTerm;
        return apiRouteRepository.searchRoutes(searchTerm, method, healthCheckEnabled)
                .doOnComplete(() -> {
                    log.info("Completed route search with term: {}", finalSearchTerm);
                })
                .doOnError(error -> log.error("Error searching routes: {}", error.getMessage()));
    }

    public Flux<ApiRoute> getAllVersions(String routeIdentifier) {
        return apiRouteVersionRepository.findByRouteIdentifierOrderByVersionDesc(routeIdentifier)
                .map(ApiRouteVersion::getRouteData)
                .doOnComplete(() -> log.info("Fetched all versions for route: {}", routeIdentifier));
    }

    public Mono<ApiRoute> getSpecificVersion(String routeIdentifier, Integer version) {
        return apiRouteVersionRepository.findByRouteIdentifierAndVersion(routeIdentifier, version)
                .map(ApiRouteVersion::getRouteData)
                .doOnSuccess(route -> {
                    if (route != null) {
                        log.info("Found version {} of route: {}", version, routeIdentifier);
                    } else {
                        log.info("Version {} of route {} not found", version, routeIdentifier);
                    }
                })
                .doOnError(error ->
                        log.error("Error fetching version {} of route {}: {}",
                                version, routeIdentifier, error.getMessage()));
    }

    public Mono<VersionComparisonResult> compareVersions(String routeIdentifier, Integer version1, Integer version2) {
        return apiRouteRepository.findByRouteIdentifier(routeIdentifier)
                .switchIfEmpty(Mono.error(new RuntimeException(
                        String.format("Route '%s' not found", routeIdentifier))))
                .flatMap(route -> Mono.zip(
                                getSpecificVersion(routeIdentifier, version1),
                                getSpecificVersion(routeIdentifier, version2)
                        ).switchIfEmpty(Mono.error(new RuntimeException(
                                String.format("One or both versions (%d, %d) not found", version1, version2))))
                        .map(tuple -> {
                            ApiRoute oldVersion = tuple.getT1();
                            ApiRoute newVersion = tuple.getT2();
                            return compareRouteVersions(oldVersion, newVersion);
                        }));
    }

    public Mono<ApiRoute> rollbackToVersion(String routeIdentifier, Integer version) {
        return getSpecificVersion(routeIdentifier, version)
                .flatMap(oldVersion -> {
                    ApiRoute rollbackRoute = cloneRoute(oldVersion);
                    return apiRouteVersionRepository.findFirstByRouteIdentifierOrderByVersionDesc(routeIdentifier)
                            .map(latestVersion -> {
                                rollbackRoute.setVersion(latestVersion.getVersion() + 1);
                                return rollbackRoute;
                            })
                            .defaultIfEmpty(rollbackRoute)
                            .flatMap(route ->
                                    generateChangeDetails(oldVersion, route,
                                            String.format("Rolled back to version %d", version))
                                            .flatMap(details -> saveVersionMetadata(
                                                    route,
                                                    details,
                                                    RouteVersionMetadata.ChangeType.ROLLBACK
                                            ))
                            )
                            .flatMap(this::updateRoute);
                })
                .doOnSuccess(route -> log.info("Successfully rolled back route {} to version {}",
                        routeIdentifier, version))
                .doOnError(error -> log.error("Error rolling back route {} to version {}: {}",
                        routeIdentifier, version, error.getMessage()));
    }

    private VersionComparisonResult compareRouteVersions(ApiRoute oldVersion, ApiRoute newVersion) {
        JsonNode oldNode = objectMapper.valueToTree(oldVersion);
        JsonNode newNode = objectMapper.valueToTree(newVersion);

        Map<String, VersionComparisonResult.FieldDifference> differences = new HashMap<>();
        List<String> addedFields = new ArrayList<>();
        List<String> removedFields = new ArrayList<>();

        compareNodes("", oldNode, newNode, differences, addedFields, removedFields);

        return VersionComparisonResult.builder()
                .routeIdentifier(oldVersion.getRouteIdentifier())
                .version1(oldVersion.getVersion())
                .version2(newVersion.getVersion())
                .differences(differences)
                .addedFields(addedFields)
                .removedFields(removedFields)
                .build();
    }

    private void compareNodes(String path, JsonNode oldNode, JsonNode newNode,
                              Map<String, VersionComparisonResult.FieldDifference> differences,
                              List<String> addedFields,
                              List<String> removedFields) {
        Iterator<String> fieldNames = oldNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;

            if (!newNode.has(fieldName)) {
                removedFields.add(fieldPath);
                continue;
            }

            JsonNode oldValue = oldNode.get(fieldName);
            JsonNode newValue = newNode.get(fieldName);

            if (!oldValue.equals(newValue)) {
                if (oldValue.isObject() && newValue.isObject()) {
                    compareNodes(fieldPath, oldValue, newValue, differences, addedFields, removedFields);
                } else {
                    differences.put(fieldPath, VersionComparisonResult.FieldDifference.builder()
                            .fieldPath(fieldPath)
                            .oldValue(objectMapper.convertValue(oldValue, Object.class))
                            .newValue(objectMapper.convertValue(newValue, Object.class))
                            .build());
                }
            }
        }

        // Check for added fields
        fieldNames = newNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
            if (!oldNode.has(fieldName)) {
                addedFields.add(fieldPath);
            }
        }
    }

    private ApiRoute cloneRoute(ApiRoute source) {
        return objectMapper.convertValue(
                objectMapper.valueToTree(source),
                ApiRoute.class
        );
    }

    private Mono<RouteChangeDetails> generateChangeDetails(ApiRoute oldVersion, ApiRoute newVersion, String summary) {
        Map<String, Object> changedFields = new HashMap<>();

        if (oldVersion != null) {
            JsonNode oldNode = objectMapper.valueToTree(oldVersion);
            JsonNode newNode = objectMapper.valueToTree(newVersion);
            compareNodesForChanges("", oldNode, newNode, changedFields);
        }

        StringBuilder description = new StringBuilder(summary);
        if (!changedFields.isEmpty()) {
            description.append("\nChanged fields:\n");
            changedFields.forEach((key, value) ->
                    description.append(String.format("- %s: %s\n", key, value)));
        }

        RouteChangeDetails details = RouteChangeDetails.builder()
                .summary(summary)
                .description(description.toString())
                .changedFields(changedFields)
                .build();

        return Mono.just(details);
    }

    private void compareNodesForChanges(String path, JsonNode oldNode, JsonNode newNode,
                                        Map<String, Object> changedFields) {
        Iterator<String> fieldNames = oldNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;

            if (!newNode.has(fieldName)) {
                changedFields.put(fieldPath, "REMOVED");
                continue;
            }

            JsonNode oldValue = oldNode.get(fieldName);
            JsonNode newValue = newNode.get(fieldName);

            if (!oldValue.equals(newValue)) {
                if (oldValue.isObject() && newValue.isObject()) {
                    compareNodesForChanges(fieldPath, oldValue, newValue, changedFields);
                } else {
                    changedFields.put(fieldPath, newValue.asText());
                }
            }
        }

        // Check for added fields
        fieldNames = newNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
            if (!oldNode.has(fieldName)) {
                changedFields.put(fieldPath, "ADDED: " + newNode.get(fieldName).asText());
            }
        }
    }

    //TODO: change the getCurrentUser to get the user from the token
    private Mono<ApiRoute> saveVersionMetadata(
            ApiRoute route,
            RouteChangeDetails changeDetails,
            RouteVersionMetadata.ChangeType changeType) {

        return userContextService.getCurrentUsername()
                .map(username -> RouteVersionMetadata.builder()
                        .routeIdentifier(route.getRouteIdentifier())
                        .version(route.getVersion())
                        .changeReason(changeDetails.getSummary())
                        .changeDescription(changeDetails.getDescription())
                        .changedFields(changeDetails.getChangedFields())
                        .changedBy(username)
                        .timestamp(System.currentTimeMillis())
                        .changeType(changeType)
                        .build()
                )
                .flatMap(metadataRepository::save)
                .thenReturn(route);
    }

    public Flux<RouteVersionMetadata> getVersionMetadata(String routeIdentifier) {
        return metadataRepository.findByRouteIdentifierOrderByVersionDesc(routeIdentifier);
    }

    public Mono<RouteExistenceResponse> checkRouteExistence(RouteExistenceRequest request) {
        String id = request.getId();
        String routeIdentifier = request.getRouteIdentifier();

        if (id == null && routeIdentifier == null) {
            return Mono.just(RouteExistenceResponse.builder()
                    .exists(false)
                    .message("No id or routeIdentifier provided")
                    .detail(RouteExistenceResponse.ExistenceDetail.builder()
                            .idExists(false)
                            .identifierExists(false)
                            .validationMessage("Both id and routeIdentifier are null")
                            .build())
                    .build());
        }

        return Mono.zip(
                id != null ? apiRouteRepository.existsById(id) : Mono.just(false),
                routeIdentifier != null ? apiRouteRepository.existsByRouteIdentifier(routeIdentifier) : Mono.just(false),
                id != null ? apiRouteRepository.findById(id) : Mono.empty(),
                routeIdentifier != null ? apiRouteRepository.findByRouteIdentifier(routeIdentifier) : Mono.empty()
        ).map(tuple -> {
            boolean idExists = tuple.getT1();
            boolean identifierExists = tuple.getT2();
            ApiRoute routeById = tuple.getT3();
            ApiRoute routeByIdentifier = tuple.getT4();

            RouteExistenceResponse.RouteDetail existingRoute = null;
            if (routeById != null) {
                existingRoute = buildRouteDetail(routeById);
            } else if (routeByIdentifier != null) {
                existingRoute = buildRouteDetail(routeByIdentifier);
            }

            return RouteExistenceResponse.builder()
                    .exists(idExists || identifierExists)
                    .message(buildExistenceMessage(idExists, identifierExists, id, routeIdentifier))
                    .detail(RouteExistenceResponse.ExistenceDetail.builder()
                            .idExists(idExists)
                            .identifierExists(identifierExists)
                            .existingId(routeById != null ? routeById.getId() : "")
                            .existingIdentifier(routeByIdentifier != null ? routeByIdentifier.getRouteIdentifier() : "")
                            .existingRoute(existingRoute)
                            .build())
                    .build();
        });
    }

    private String buildExistenceMessage(boolean idExists, boolean identifierExists,
                                         String id, String routeIdentifier) {
        StringBuilder message = new StringBuilder();
        if (idExists) {
            message.append("Route with id '").append(id).append("' already exists. ");
        }
        if (identifierExists) {
            message.append("Route with identifier '").append(routeIdentifier).append("' already exists.");
        }
        if (!idExists && !identifierExists) {
            message.append("Route is available for creation.");
        }
        return message.toString();
    }

    private RouteExistenceResponse.RouteDetail buildRouteDetail(ApiRoute route) {
        return RouteExistenceResponse.RouteDetail.builder()
                .id(route.getId())
                .routeIdentifier(route.getRouteIdentifier())
                .uri(route.getUri())
                .method(route.getMethod())
                .path(route.getPath())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .version(route.getVersion())
                .build();
    }

    private void sanitizeHealthCheckConfig(HealthCheckConfig healthCheck) {
        // Skip if health check is null
        if (healthCheck == null) {
            return;
        }

        if (healthCheck.getAlertRules() != null) {
            List<AlertRule> sanitizedRules = new ArrayList<>();
            healthCheck.getAlertRules().forEach(rule -> {
                sanitizedRules.add(rule);
            });
            healthCheck.setAlertRules(sanitizedRules);
        }
        
        if (healthCheck.getThresholds() != null) {
            HealthThresholds thresholds = healthCheck.getThresholds();
            // Handle each threshold field individually
            if (thresholds.getCpuThreshold() != null) {
                thresholds.setCpuThreshold(thresholds.getCpuThreshold());
            }
            if (thresholds.getMemoryThreshold() != null) {
                thresholds.setMemoryThreshold(thresholds.getMemoryThreshold());
            }
            if (thresholds.getResponseTimeThreshold() != null) {
                thresholds.setResponseTimeThreshold(thresholds.getResponseTimeThreshold());
            }
            if (thresholds.getTimeoutThreshold() != null) {
                thresholds.setTimeoutThreshold(thresholds.getTimeoutThreshold());
            }
        }
    }
}
