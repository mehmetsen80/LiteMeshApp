package org.lite.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.lite.gateway.exception.InvalidAuthenticationException;
import org.springframework.web.server.ServerWebExchange;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserContextService {

    public static final String SYSTEM_USER = "SYSTEM";

    private final ReactiveJwtDecoder userJwtDecoder;
    private final ReactiveJwtDecoder keycloakJwtDecoder;

    /**
     * Get current user from request headers (for controller endpoints)
     */
    public Mono<String> getCurrentUser(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(context -> {
                String userToken = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Token");

                if (userToken == null) {
                    return Mono.error(new InvalidAuthenticationException("No valid user token found"));
                }
                
                // Remove 'Bearer ' if it's still present
                String token = userToken.startsWith("Bearer ") ? 
                    userToken.substring(7) : userToken;
                
                // Try to determine if this is a Keycloak token by checking its structure
                boolean isKeycloakToken = isKeycloakToken(token);
                //log.debug("Token type: {}", isKeycloakToken ? "Keycloak" : "User");
                
                ReactiveJwtDecoder decoder = isKeycloakToken ? keycloakJwtDecoder : userJwtDecoder;
                
                return decoder.decode(token)
                    .map(jwt -> {
                        String username = isKeycloakToken ? 
                            jwt.getClaimAsString("preferred_username") : 
                            jwt.getClaimAsString("username");
                        log.debug("Decoded token claims: {}", jwt.getClaims());
                        log.debug("Username from token: {}", username);
                        if (username == null || username.trim().isEmpty()) {
                            throw new InvalidAuthenticationException("No username found in JWT token");
                        }
                        return username;
                    })
                    .onErrorResume(e -> {
                        log.error("Error decoding token: {}", e.getMessage());
                        return Mono.error(new InvalidAuthenticationException("Invalid token: " + e.getMessage()));
                    });
            });
    }
    
    /**
     * Get current user from security context (for internal service calls)
     */
    public Mono<String> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> {
                if (context.getAuthentication() == null) {
                    return SYSTEM_USER;
                }
                return context.getAuthentication().getName();
            });
    }

    public boolean isKeycloakToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            // Decode the header
            String header = new String(java.util.Base64.getDecoder().decode(parts[0]));
            
            // Check for Keycloak-specific claims or structure
            // Keycloak tokens always have a kid, and can use either RS256 (access) or HS512 (refresh)
            return header.contains("\"kid\""); // Only check for kid, don't check algorithm
        } catch (Exception e) {
            log.error("Error parsing token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            // Split the token into parts
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            // For standard login, check header for HS256 algorithm
            String header = new String(java.util.Base64.getDecoder().decode(parts[0]));
            if (header.contains("\"alg\":\"HS256\"")) {
                // This is a standard login token, verify it has a subject
                String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
                return payload.contains("\"sub\":");
            }
            
            // For SSO/Keycloak tokens, check for typ:Refresh
            String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
            return payload.contains("\"typ\":\"Refresh\"");
            
        } catch (Exception e) {
            log.error("Error parsing token: {}", e.getMessage());
            return false;
        }
    }
} 