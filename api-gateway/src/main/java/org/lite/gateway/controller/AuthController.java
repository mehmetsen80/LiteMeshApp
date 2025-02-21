package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.lite.gateway.dto.LoginRequest;
import org.lite.gateway.dto.RegisterRequest;
import org.lite.gateway.dto.AuthResponse;
import org.lite.gateway.service.UserService;
import org.lite.gateway.service.KeycloakService;
import org.lite.gateway.service.JwtService;
import org.lite.gateway.service.UserContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.lite.gateway.dto.RefreshTokenRequest;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final KeycloakService keycloakService;
    private final JwtService jwtService;
    private final UserContextService userContextService;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return userService.login(request)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                .body(AuthResponse.builder()
                    .message(e.getMessage())
                    .build())));
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@RequestBody RegisterRequest request) {
        return userService.register(request)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                .body(AuthResponse.builder()
                    .message(e.getMessage())
                    .build())));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        boolean isKeycloakToken = userContextService.isKeycloakToken(request.getRefresh_token());
        log.info("Token type: {}", isKeycloakToken ? "Keycloak" : "Standard");

        Mono<AuthResponse> refreshMono = isKeycloakToken ?
            keycloakService.refreshToken(request.getRefresh_token()) :
            userService.refreshToken(request.getRefresh_token());

        return refreshMono
            .doOnNext(response -> log.info("Refresh response: {}", response))
            .flatMap(authResponse -> {
                if (authResponse.isExpired()) {
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(authResponse));
                }
                
                if (authResponse.getToken() == null) {
                    return Mono.just(ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                            .message("Failed to refresh token")
                            .success(false)
                            .build()));
                }

                return jwtService.extractClaims(authResponse.getToken())
                    .flatMap(claims -> {
                        String username = claims.get("preferred_username", String.class);
                        String email = claims.get("email", String.class);
                        List<String> roles = claims.get("realm_access", Map.class) != null ?
                            ((Map<String, List<String>>) claims.get("realm_access", Map.class)).get("roles") :
                            new ArrayList<>();
                        
                        Map<String, Object> user = new HashMap<>();
                        user.put("username", username);
                        user.put("email", email);
                        user.put("roles", roles);

                        return Mono.just(ResponseEntity.ok(AuthResponse.builder()
                            .token(authResponse.getToken())
                            .refreshToken(authResponse.getRefreshToken())
                            .user(user)
                            .success(true)
                            .build()));
                    });
            })
            .doOnError(e -> log.error("Token refresh error", e));
    }

    @PostMapping("/test-refresh")
    public Mono<ResponseEntity<AuthResponse>> testRefresh(@RequestBody RefreshTokenRequest request) {
        log.info("Testing token refresh with token: {}", 
            request.getRefresh_token() != null ? request.getRefresh_token().substring(0, 10) + "..." : "null");
        return refreshToken(request);
    }
} 