package org.lite.gateway.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.LinqRequest;
import org.lite.gateway.dto.LinqResponse;
import org.lite.gateway.entity.RoutePermission;
import org.lite.gateway.service.LinqService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.lite.gateway.repository.ApiRouteRepository;
import org.lite.gateway.repository.TeamRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.NonNull;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinqServiceImpl implements LinqService {

    @NonNull
    private final WebClient.Builder webClientBuilder;

    @NonNull
    private final RedisTemplate<String, String> redisTemplate;

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final ApiRouteRepository apiRouteRepository;

    @NonNull
    private final TeamRouteRepository teamRouteRepository;

    @Value("${spring.server.host:localhost}")
    private String gatewayHost;

    @Value("${spring.server.port:7777}")
    private int gatewayPort;

    @Value("${spring.server.ssl.enabled:true}")
    private boolean sslEnabled;

    @Override
    public Mono<LinqResponse> processLinqRequest(LinqRequest request) {
        return validateRoutePermission(request)
            .then(Mono.defer(() -> {
                if (request.getQuery() == null || request.getQuery().getIntent().isEmpty()) {
                    return Mono.error(new IllegalArgumentException("Invalid LINQ request"));
                }

                String cacheKey = generateCacheKey(request);

                return Mono.fromCallable(() -> redisTemplate.opsForValue().get(cacheKey))
                        .filter(Objects::nonNull)
                        .map(value -> {
                            try {
                                return objectMapper.readValue(value, LinqResponse.class);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to deserialize cached LinqResponse", e);
                            }
                        })
                        .map(response -> {
                            response.getMetadata().setCacheHit(true);
                            return response;
                        })
                        .switchIfEmpty(
                                executeLinqRequest(request)
                                        .doOnNext(response -> {
                                            try {
                                                String jsonResponse = objectMapper.writeValueAsString(response);
                                                redisTemplate.opsForValue().set(cacheKey, jsonResponse, Duration.ofMinutes(5));
                                            } catch (Exception e) {
                                                throw new RuntimeException("Failed to serialize LinqResponse for cache", e);
                                            }
                                        })
                        );
            }));
    }

    private Mono<Void> validateRoutePermission(LinqRequest request) {
        String routeIdentifier = request.getLink().getTarget();
        
        return getTeamFromContext()
            .flatMap(teamId -> {
                String permissionCacheKey = String.format("permission:%s:%s", teamId, routeIdentifier);
                
                return Mono.fromCallable(() -> redisTemplate.opsForValue().get(permissionCacheKey))
                    .filter(Objects::nonNull)
                    .switchIfEmpty(checkAndCachePermission(routeIdentifier, permissionCacheKey, teamId))
                    .filter(Boolean::parseBoolean)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, 
                        "Team does not have USE permission for " + routeIdentifier)))
                    .then();
            });
    }

    private Mono<String> checkAndCachePermission(String routeIdentifier, String cacheKey, String teamId) {
        return apiRouteRepository.findByRouteIdentifier(routeIdentifier)
            .flatMap(apiRoute -> 
                teamRouteRepository.findByTeamIdAndRouteId(teamId, apiRoute.getId())
                    .map(teamRoute -> {
                        boolean hasUsePermission = 
                            teamRoute.getPermissions().contains(RoutePermission.USE);
                        redisTemplate.opsForValue().set(cacheKey, 
                            String.valueOf(hasUsePermission), 
                            Duration.ofMinutes(5));
                        return String.valueOf(hasUsePermission);
                    })
            )
            .switchIfEmpty(Mono.just("false"));
    }

    private String generateCacheKey(LinqRequest request) {
        return "linq:" + request.getQuery().hashCode();
    }

    private Mono<LinqResponse> executeLinqRequest(LinqRequest request) {
        String target = request.getLink().getTarget();
        String intent = request.getQuery().getIntent();
        String url = buildUrl(target, intent, request.getQuery().getParams());
        String action = request.getLink().getAction().toLowerCase();
        String method = switch (action) {
            case "fetch" -> "GET";
            case "create" -> "POST";
            case "update" -> "PUT";
            case "delete" -> "DELETE";
            default -> throw new IllegalArgumentException("Unsupported action: " + action);
        };

        return invokeService(method, url, request)
            .flatMap(result -> 
                getTeamFromContext()
                    .map(team -> {
                        LinqResponse response = new LinqResponse();
                        response.setResult(result);
                        LinqResponse.Metadata metadata = new LinqResponse.Metadata();
                        metadata.setSource(target);
                        metadata.setStatus("success");
                        metadata.setTeam(team);
                        metadata.setCacheHit(false);
                        response.setMetadata(metadata);
                        return response;
                    })
            );
    }

    private String buildUrl(String target, String intent, Map<String, String> params) {
        // Dynamically build baseUrl with protocol based on SSL config
        String protocol = sslEnabled ? "https" : "http";
        String baseUrl = String.format("%s://%s:%d", protocol, gatewayHost, gatewayPort);
        String url = baseUrl + "/" + target + "/" + intent;
        if (params != null && !params.isEmpty()) {
            url += "?" + params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
        }
        return url;
    }

    private Mono<Object> invokeService(String method, String url, LinqRequest request) {
        return getApiKeyFromContext()
            .flatMap(apiKey -> {
                log.debug("Making {} request to {} with API key present", method, url);
                
                WebClient webClient = webClientBuilder.build();
                WebClient.RequestHeadersSpec<?> requestSpec = switch (method) {
                    case "GET" -> webClient.get().uri(url);
                    case "POST" -> webClient.post().uri(url)
                            .bodyValue(request.getQuery().getParams());
                    case "PUT" -> webClient.put().uri(url)
                            .bodyValue(request.getQuery().getParams());
                    case "DELETE" -> webClient.delete().uri(url);
                    default -> throw new IllegalArgumentException("Method not supported: " + method);
                };

                return requestSpec
                    .header("X-API-Key", apiKey)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(Object.class)
                                .switchIfEmpty(Mono.just(Map.of("message", "Success but no content")));
                        } else {
                            return response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException(
                                    "Service returned " + response.statusCode() + ": " + error)));
                        }
                    });
            })
            .doOnError(error -> log.error("Error calling service {}: {}", url, error.getMessage()))
            .onErrorResume(error -> Mono.just(Map.of("error", error.getMessage())));
    }

    private Mono<String> getApiKeyFromContext() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(auth -> auth instanceof UsernamePasswordAuthenticationToken)
            .map(auth -> {
                Object credentials = auth.getCredentials();
                if (credentials instanceof String) {
                    log.debug("Found API key in security context");
                    return (String) credentials;
                }
                log.error("Invalid credentials type in security context: {}", 
                    credentials != null ? credentials.getClass() : "null");
                throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid API key format");
            })
            .switchIfEmpty(Mono.error(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "No valid API key authentication found")));
    }

    private Mono<String> getTeamFromContext() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .cast(String.class);
    }
}