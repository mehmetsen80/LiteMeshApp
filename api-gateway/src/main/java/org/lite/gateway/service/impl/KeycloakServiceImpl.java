package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.config.KeycloakProperties;
import org.lite.gateway.dto.AuthResponse;
import org.lite.gateway.service.JwtService;
import org.lite.gateway.service.KeycloakService;
import org.lite.gateway.service.UserService;
import org.lite.gateway.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.lite.gateway.service.CodeCacheService;
import org.lite.gateway.exception.InvalidAuthenticationException;
import org.lite.gateway.dto.ErrorCode;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakServiceImpl implements KeycloakService {

    private final WebClient.Builder webClientBuilder;
    private final KeycloakProperties keycloakProperties;
    private final JwtService jwtService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final CodeCacheService codeCacheService;

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
        log.info("\n=== Starting OAuth Token Exchange ===\n" +
            "Full code from callback: {}\n" +
            "Code parts: {}\n" +
            "Token URL: {}", 
            code,
            code.split("\\."),
            keycloakProperties.getTokenUrl().block());

        return Mono.zip(
            keycloakProperties.getTokenUrl(),
            keycloakProperties.getClientId(),
            keycloakProperties.getClientSecret()
        ).flatMap(tuple -> {
            String tokenUrl = tuple.getT1();
            String clientId = tuple.getT2();
            String clientSecret = tuple.getT3();
            
            String redirectUri = "http://localhost:3000/callback";
            
            log.info("\n=== OAuth Configuration ===\n" +
                "Token URL: {}\n" +
                "Client ID: {}\n" +
                "Client Secret (first 4 chars): {}\n" +
                "Redirect URI: {}", 
                tokenUrl,
                clientId,
                clientSecret.substring(0, 4),
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
        return Mono.just(tokenResponse)
            .flatMap(response -> {
                String username = jwtService.extractUsername(response.getAccessToken());
                return userService.findByUsername(username)
                    .switchIfEmpty(createUserFromKeycloak(username, response.getAccessToken()))
                    .map(user -> AuthResponse.builder()
                        .username(user.getUsername())
                        .token(response.getAccessToken())
                        .build());
            });
    }

    private Mono<User> createUserFromKeycloak(String username, String token) {
        return Mono.just(new User())
            .map(user -> {
                user.setUsername(username);
                user.setEmail(extractEmailFromToken(token));
                user.setActive(true);
                user.setPassword(""); // Set an empty password since we're using SSO
                return user;
            })
            .flatMap(userService::save);
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