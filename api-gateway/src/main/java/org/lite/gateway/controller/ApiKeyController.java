package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.lite.gateway.dto.CreateApiKeyRequest;
import org.lite.gateway.dto.ApiKeyResponse;
import org.lite.gateway.entity.ApiKey;
import org.lite.gateway.repository.ApiKeyRepository;
import org.lite.gateway.service.ApiKeyService;
import org.lite.gateway.service.UserContextService;
import org.lite.gateway.service.TeamService;
import org.lite.gateway.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {
    private final ApiKeyService apiKeyService;
    private final UserContextService userContextService;
    private final TeamService teamService;
    private final UserService userService;
    private final ApiKeyRepository apiKeyRepository;

    @PostMapping
    public Mono<ApiKeyResponse> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request,
            ServerWebExchange exchange) {
        return userContextService.getCurrentUsername(exchange)
            .flatMap(userService::findByUsername)
            .flatMap(user -> 
                teamService.hasRole(request.getTeamId(), user.getId(), "ADMIN")
                    .filter(hasRole -> hasRole || user.getRoles().contains("SUPER_ADMIN"))
                    .switchIfEmpty(Mono.error(new AccessDeniedException(
                        "Admin access required for team " + request.getTeamId())))
                    .map(hasRole -> user.getUsername())
            )
            .flatMap(username -> apiKeyService.createApiKey(
                request.getName(),
                request.getTeamId(),
                username,
                request.getExpiresInDays()
            ))
            .map(this::toApiKeyResponse);
    }

    @PostMapping("/{id}/revoke")
    public Mono<Void> revokeApiKey(@PathVariable String id, ServerWebExchange exchange) {
        return apiKeyRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("API key not found")))
            .flatMap(apiKey -> 
                userContextService.getCurrentUsername(exchange)
                    .flatMap(userService::findByUsername)
                    .flatMap(user ->
                        teamService.hasRole(apiKey.getTeamId(), user.getId(), "ADMIN")
                            .filter(hasRole -> hasRole || user.getRoles().contains("SUPER_ADMIN"))
                            .switchIfEmpty(Mono.error(new AccessDeniedException(
                                "Admin access required for team " + apiKey.getTeamId())))
                    )
                    .then(apiKeyService.revokeApiKey(id))
            );
    }

    @GetMapping
    public Mono<List<ApiKeyResponse>> getApiKeys(
            @RequestParam String teamId,
            ServerWebExchange exchange) {
        return userContextService.getCurrentUsername(exchange)
            .flatMap(userService::findByUsername)
            .flatMap(user ->
                teamService.hasRole(teamId, user.getId(), "ADMIN")
                    .filter(hasRole -> hasRole || user.getRoles().contains("SUPER_ADMIN"))
                    .switchIfEmpty(Mono.error(new AccessDeniedException(
                        "Admin access required for team " + teamId)))
                    .thenMany(apiKeyRepository.findByTeamId(teamId))
                    .map(this::toApiKeyResponse)
                    .collectList()
            );
    }

    @DeleteMapping("/{id}")
    public Mono<Void> removeApiKey(@PathVariable String id, ServerWebExchange exchange) {
        return apiKeyRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("API key not found")))
            .flatMap(apiKey -> 
                userContextService.getCurrentUsername(exchange)
                    .flatMap(userService::findByUsername)
                    .flatMap(user ->
                        teamService.hasRole(apiKey.getTeamId(), user.getId(), "ADMIN")
                            .filter(hasRole -> hasRole || user.getRoles().contains("SUPER_ADMIN"))
                            .switchIfEmpty(Mono.error(new AccessDeniedException(
                                "Admin access required for team " + apiKey.getTeamId())))
                    )
                    .then(apiKeyRepository.deleteById(id))
            );
    }

    private ApiKeyResponse toApiKeyResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
            .id(apiKey.getId())
            .key(apiKey.getKey())
            .name(apiKey.getName())
            .teamId(apiKey.getTeamId())
            .createdBy(apiKey.getCreatedBy())
            .createdAt(apiKey.getCreatedAt())
            .expiresAt(apiKey.getExpiresAt())
            .enabled(apiKey.isEnabled())
            .build();
    }
} 