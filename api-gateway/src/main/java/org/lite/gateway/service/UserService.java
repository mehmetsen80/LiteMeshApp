package org.lite.gateway.service;

import org.lite.gateway.dto.AuthResponse;
import org.lite.gateway.dto.LoginRequest;
import org.lite.gateway.dto.RegisterRequest;
import org.lite.gateway.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Map;

public interface UserService {
    
    Mono<AuthResponse> login(LoginRequest request);
    
    Mono<AuthResponse> register(RegisterRequest request);
    
    Mono<User> findByUsername(String username);
    
    Mono<User> findByUsernameForLogin(String username);
    
    Flux<User> searchUsers(String query);
    
    Mono<User> findById(String id);
    
    Mono<Map<String, Object>> validatePasswordStrength(String password);
    
    Mono<User> save(User user);

    Mono<AuthResponse> refreshToken(String token);

    Mono<User> createUserFromKeycloak(String username, String email);

    Mono<User> createUserIfNotExists(String username, String email);
} 