package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.AuthResponse;
import org.lite.gateway.dto.ErrorCode;
import org.lite.gateway.dto.LoginRequest;
import org.lite.gateway.dto.RegisterRequest;
import org.lite.gateway.entity.User;
import org.lite.gateway.exception.DuplicateUserException;
import org.lite.gateway.exception.ResourceNotFoundException;
import org.lite.gateway.exception.ValidationException;
import org.lite.gateway.repository.UserRepository;
import org.lite.gateway.service.JwtService;
import org.lite.gateway.service.UserService;
import org.lite.gateway.util.UserValidationUtil;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.lite.gateway.service.UserContextService;
import org.lite.gateway.exception.InvalidCredentialsException;

import java.util.HashMap;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserContextService userContextService;
    private final TransactionalOperator transactionalOperator;

    public Mono<AuthResponse> login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsername());
        
        // Try both username and email
        return Mono.just(request.getUsername())
            .flatMap(login -> {
                if (login.contains("@")) {
                    log.info("Attempting email login for: {}", login);
                    return userRepository.findByEmail(login);
                }
                log.info("Attempting username login for: {}", login);
                return userRepository.findByUsername(login);
            })
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("username '" + request.getUsername() + "' does not exist"
            , ErrorCode.USER_NOT_FOUND)
            ))
            .flatMap(user -> {
                if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                    String token = jwtService.generateToken(user.getUsername());
                    String refreshToken = jwtService.generateRefreshToken(user.getUsername());
                    
                    Map<String, Object> userMap = convertToUserMap(user);
                    
                    log.info("Login successful for user: {}", user.getUsername());
                    return Mono.just(AuthResponse.builder()
                        .token(token)
                        .refreshToken(refreshToken)
                        .user(userMap)
                        .success(true)
                        .message("Login successful")
                        .build());
                }
                log.info("Invalid password for user: {}", user.getUsername());
                return Mono.error(new InvalidCredentialsException(ErrorCode.USER_INVALID_CREDENTIALS));
            })
            .doOnError(e -> log.error("Login error: ", e));
    }

    public Mono<AuthResponse> register(RegisterRequest request) {
        try {
            UserValidationUtil.validateRegistration(request);
        } catch (ValidationException e) {
            return Mono.error(e);
        }

        Mono<AuthResponse> registrationMono = userRepository.existsByUsername(request.getUsername())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new DuplicateUserException(
                        ErrorCode.USER_ALREADY_EXISTS.getDefaultMessage()
                    ));
                }
                return userRepository.existsByEmail(request.getEmail());
            })
            .flatMap(emailExists -> {
                if (emailExists) {
                    return Mono.error(new DuplicateUserException(
                        ErrorCode.USER_EMAIL_EXISTS.getDefaultMessage()
                    ));
                }

                User newUser = new User();
                newUser.setUsername(request.getUsername());
                newUser.setPassword(passwordEncoder.encode(request.getPassword()));
                newUser.setEmail(request.getEmail());
                newUser.setRoles(Set.of("USER"));

                return userRepository.save(newUser)
                    .map(savedUser -> {
                        String token = jwtService.generateToken(savedUser.getUsername());
                        String refreshToken = jwtService.generateRefreshToken(savedUser.getUsername());

                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("username", savedUser.getUsername());
                        userMap.put("email", savedUser.getEmail());
                        userMap.put("roles", savedUser.getRoles());
                        userMap.put("id", savedUser.getId());
                        userMap.put("authType", "LOCAL");

                        return AuthResponse.builder()
                            .token(token)
                            .refreshToken(refreshToken)
                            .user(userMap)
                            .message("Registration successful")
                            .success(true)
                            .build();
                    });
            });

        return transactionalOperator.execute(status -> registrationMono)
            .single()
            .doOnSuccess(response -> log.info("Transaction completed successfully"))
            .doOnError(e -> log.error("Transaction failed: {}", e.getMessage()))
            .onErrorMap(e -> {
                if (e.getMessage() != null && e.getMessage().contains("duplicate key error")) {
                    if (e.getMessage().contains("email_1")) {
                        return new DuplicateUserException(
                            ErrorCode.USER_EMAIL_EXISTS.getDefaultMessage()
                        );
                    }
                    if (e.getMessage().contains("username_1")) {
                        return new DuplicateUserException(
                            ErrorCode.USER_ALREADY_EXISTS.getDefaultMessage()
                        );
                    }
                }
                return e;
            });
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Mono<User> findByUsernameForLogin(String username) {
        log.info("Finding user by username for login: {}", username);
        return userRepository.findByUsername(username)
            .doOnNext(user -> log.info("Found user: {}", user))
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                "username '" + username + "' does not exist",
                ErrorCode.USER_NOT_FOUND
            )))
            .doOnError(error -> log.error("Error finding user by username: {}", error.getMessage()));
    }



    public Flux<User> searchUsers(String query) {
        log.debug("Searching users by username or email containing: {}", query);
        return userRepository.findAll()
            .filter(user -> 
                user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                (user.getEmail() != null && 
                 user.getEmail().toLowerCase().contains(query.toLowerCase()))
            )
            .doOnComplete(() -> log.debug("Completed user search"))
            .doOnError(error -> log.error("Error searching users: {}", error.getMessage()));
    }

    public Mono<User> findById(String id) {
        log.debug("Finding user by id: {}", id);
        return userRepository.findById(id)
            .doOnNext(user -> log.debug("Found user: {}", user))
            .doOnError(error -> log.error("Error finding user by id: {}", error.getMessage()));
    }

    public Mono<Map<String, Object>> validatePasswordStrength(String password) {
        Map<String, Object> result = new HashMap<>();
        String error = UserValidationUtil.getPasswordStrengthError(password);
        result.put("isStrong", error == null);
        result.put("error", error);
        return Mono.just(result);
    }

    @Override
    public Mono<AuthResponse> refreshToken(String token) {
        log.info("Attempting to refresh standard user token: {}", token);
        
        try {
            // Verify this is a refresh token
            if (!userContextService.isRefreshToken(token)) {
                log.error("Invalid token type - not a refresh token");
                return Mono.just(AuthResponse.builder()
                    .message("Invalid token type - expected refresh token")
                    .success(false)
                    .build());
            }

            // Extract username from token
            String username = jwtService.getUsernameFromToken(token);
            log.info("Refreshing token for user: {}", username);

            // Generate new tokens
            String newToken = jwtService.generateToken(username);
            String newRefreshToken = jwtService.generateRefreshToken(username);

            // Get user details
            return userRepository.findByUsername(username)
                .map(user -> {
                    Map<String, Object> userMap = convertToUserMap(user);
                    
                    return AuthResponse.builder()
                        .token(newToken)
                        .refreshToken(newRefreshToken)
                        .user(userMap)
                        .success(true)
                        .message("Token refreshed successfully")
                        .build();
                })
                .switchIfEmpty(Mono.just(AuthResponse.builder()
                    .message("User not found")
                    .success(false)
                    .build()));
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return Mono.just(AuthResponse.builder()
                .message("Failed to refresh token: " + e.getMessage())
                .success(false)
                .build());
        }
    }

    public Mono<User> createUserFromKeycloak(String username, String email) {
        return Mono.just(new User())
            .map(user -> {
                user.setUsername(username);
                user.setEmail(email);
                user.setActive(true);
                user.setPassword(""); // Set an empty password since we're using SSO
                return user;
            })
            .flatMap(userRepository::save);
    }



//    public Mono<User> createUserIfNotExists(String username, String email) {
//        return findByUsername(username)
//            .switchIfEmpty(createUserFromKeycloak(username, email));
//    }

    private Map<String, Object> convertToUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("roles", user.getRoles());
        userMap.put("id", user.getId());
        userMap.put("authType", "LOCAL");
        return userMap;
    }
}
