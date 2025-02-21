package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.config.KeycloakProperties;
import org.lite.gateway.dto.AuthResponse;
import org.lite.gateway.service.JwtService;
import org.lite.gateway.service.KeycloakService;
import org.lite.gateway.service.UserContextService;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.lite.gateway.service.CodeCacheService;
import org.lite.gateway.exception.InvalidAuthenticationException;
import org.lite.gateway.exception.TokenExpiredException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;


@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakServiceImpl implements KeycloakService {

    private final WebClient.Builder webClientBuilder;
    private final KeycloakProperties keycloakProperties;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final CodeCacheService codeCacheService;
    private final UserContextService userContextService;

    @Override
    public Mono<AuthResponse> handleCallback(String code) {
        log.info("\n=== Starting OAuth Token Exchange ===\nFull code from callback: {}", code);
        
        return codeCacheService.isCodeUsed(code)
            .flatMap(isUsed -> {
                if (isUsed) {
                    log.debug("Code already used, checking for existing session");
                    // Try to find existing user session
                    return exchangeCodeForToken(code)
                        .flatMap(this::validateAndCreateSession)
                        .onErrorResume(error -> {
                            log.error("Error processing used code:", error);
                            return Mono.<AuthResponse>error(new InvalidAuthenticationException(
                                "Code already in use"
                            ));
                        });
                }
                
                return codeCacheService.markCodeAsUsed(code)
                    .flatMap(marked -> {
                        if (!marked) {
                            log.warn("Failed to mark code as used (race condition): {}", code);
                            return Mono.<AuthResponse>error(new InvalidAuthenticationException(
                                "Code already in use"
                            ));
                        }
                        
                        return exchangeCodeForToken(code)
                            .flatMap(this::validateAndCreateSession);
                    });
            });
    }

    private Mono<KeycloakTokenResponse> exchangeCodeForToken(String code) {
        log.debug("Starting token exchange for code: {}", code);
        
        return Mono.zip(
            keycloakProperties.getTokenUrl()
                .doOnError(e -> log.error("Failed to get token URL: {}", e.getMessage())),
            keycloakProperties.getClientId()
                .doOnError(e -> log.error("Failed to get client ID: {}", e.getMessage())),
            keycloakProperties.getClientSecret()
                .doOnError(e -> log.error("Failed to get client secret: {}", e.getMessage())),
            keycloakProperties.getRedirectUri()
                .doOnError(e -> log.error("Failed to get redirect URI: {}", e.getMessage()))
        )
        .doOnError(error -> log.error("Failed to get OAuth2 properties: {}", error.getMessage()))
        .flatMap(tuple -> {
            String tokenUrl = tuple.getT1();
            String clientId = tuple.getT2();
            String clientSecret = tuple.getT3();
            String redirectUri = tuple.getT4();
            
            if (tokenUrl == null || clientId == null || clientSecret == null || redirectUri == null) {
                return Mono.error(new IllegalStateException("One or more OAuth2 properties are null"));
            }

            log.info("\n=== OAuth Configuration ===\n" +
                "Token URL: {}\n" +
                "Client ID: {}\n" +
                "Client Secret (first 4 chars): {}\n" +
                "Redirect URI: {}", 
                tokenUrl,
                clientId,
                clientSecret.substring(0, Math.min(4, clientSecret.length())),
                redirectUri);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "authorization_code");
            formData.add("code", code);
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("redirect_uri", redirectUri);
            
            return webClientBuilder.build().post()
                .uri(tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .bodyValue(formData)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError()) {
                        return response.bodyToMono(String.class)
                            .flatMap(error -> {
                                log.error("\n=== Token Request Failed ===\n" +
                                    "Status: {}\n" +
                                    "Error: {}\n" +
                                    "Headers: {}\n" +
                                    "Request Details:\n" +
                                    "  URL: {}\n" +
                                    "  Client ID: {}\n" +
                                    "  Redirect URI: {}\n" +
                                    "  Code Length: {}", 
                                    response.statusCode(), 
                                    error,
                                    response.headers().asHttpHeaders(),
                                    tokenUrl,
                                    clientId,
                                    redirectUri,
                                    code.length());
                                return Mono.error(new RuntimeException("Token request failed: " + error));
                            });
                    }
                    log.info("Token request successful");
                    return response.bodyToMono(KeycloakTokenResponse.class);
                })
                .doOnNext(response -> log.info("Successfully received token response"))
                .doOnError(error -> log.error("Error exchanging code for token", error));
        });
    }

    private Mono<AuthResponse> validateAndCreateSession(KeycloakTokenResponse tokenResponse) {
        return jwtService.extractClaims(tokenResponse.getAccessToken())
            .map(claims -> {
                String username = claims.get("preferred_username", String.class);
                String email = claims.get("email", String.class);
                List<String> roles = claims.get("realm_access", Map.class) != null ?
                    ((Map<String, List<String>>) claims.get("realm_access", Map.class)).get("roles") :
                    new ArrayList<>();

                Map<String, Object> user = new HashMap<>();
                user.put("username", username);
                user.put("email", email);
                user.put("roles", roles);

                return AuthResponse.builder()
                    .token(tokenResponse.getAccessToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .user(user)
                    .success(true)
                    .build();
            });
    }

    private String extractEmailFromToken(String token) {
        try {
            String[] chunks = token.split("\\.");
            String payload = new String(Base64.getDecoder().decode(chunks[1]));
            JsonNode jsonNode = objectMapper.readTree(payload);
            return jsonNode.get("email").asText();
        } catch (Exception e) {
            log.error("Failed to extract email from token", e);
            return null;
        }
    }


    @Override
    public Mono<AuthResponse> refreshToken(String refreshToken) {
        log.info("Attempting to refresh token with Keycloak");
        
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Refresh token is required"));
        }

        // Verify it's actually a refresh token
        if (!userContextService.isRefreshToken(refreshToken)) {
            log.error("Invalid token type - expected refresh token");
            return Mono.just(AuthResponse.builder()
                .message("Invalid token type - expected refresh token")
                .success(false)
                .build());
        }

        return Mono.zip(
            keycloakProperties.getClientId(),
            keycloakProperties.getClientSecret(),
            keycloakProperties.getTokenUrl()
        ).flatMap(tuple -> {
            String clientId = tuple.getT1();
            String clientSecret = tuple.getT2();
            String tokenUrl = tuple.getT3();
            
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "refresh_token");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("refresh_token", refreshToken);

            return webClientBuilder.build()
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> response.bodyToMono(String.class)
                        .flatMap(error -> {
                            log.error("Keycloak refresh token error: {}", error);
                            if (error.contains("Token is not active")) {
                                return Mono.error(new TokenExpiredException("Refresh token has expired"));
                            }
                            return Mono.error(new RuntimeException("Token refresh failed: " + error));
                        })
                )
                .bodyToMono(KeycloakTokenResponse.class)
                .flatMap(response -> {
                    // Extract claims from the token
                    return jwtService.extractClaims(response.getAccessToken())
                        .map(claims -> {
                            String username = claims.get("preferred_username", String.class);
                            String email = claims.get("email", String.class);
                            List<String> roles = claims.get("realm_access", Map.class) != null ?
                                ((Map<String, List<String>>) claims.get("realm_access", Map.class)).get("roles") :
                                new ArrayList<>();

                            Map<String, Object> user = new HashMap<>();
                            user.put("username", username);
                            user.put("email", email);
                            user.put("roles", roles);

                            return AuthResponse.builder()
                                .token(response.getAccessToken())
                                .refreshToken(response.getRefreshToken())
                                .user(user)
                                .success(true)
                                .build();
                        });
                })
                .onErrorResume(e -> {
                    if (e instanceof TokenExpiredException) {
                        return Mono.just(AuthResponse.builder()
                            .message("Session expired. Please login again.")
                            .success(false)
                            .expired(true)
                            .build());
                    }
                    log.error("Token refresh failed", e);
                    return Mono.just(AuthResponse.builder()
                        .message(e.getMessage())
                        .success(false)
                        .build());
                });
        });
    }
}

record KeycloakTokenRequest(
    String code,
    String clientId,
    String clientSecret,
    String redirectUri,
    String grantType
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record KeycloakTokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Integer expiresIn
) {
    @JsonProperty("access_token")
    public String getAccessToken() { return accessToken; }
    
    @JsonProperty("refresh_token")
    public String getRefreshToken() { return refreshToken; }
    
    @JsonProperty("token_type")
    public String getTokenType() { return tokenType; }
    
    @JsonProperty("expires_in")
    public Integer getExpiresIn() { return expiresIn; }
} 