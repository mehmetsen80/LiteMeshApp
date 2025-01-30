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
                
                // Decode and validate the user token
                return userJwtDecoder.decode(token)
                    .map(jwt -> {
                        String username = jwt.getClaimAsString("username");
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
} 