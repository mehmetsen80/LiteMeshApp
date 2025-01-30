package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.AuthResponse;
import org.lite.gateway.dto.ErrorCode;
import org.lite.gateway.dto.LoginRequest;
import org.lite.gateway.dto.RegisterRequest;
import org.lite.gateway.entity.User;
import org.lite.gateway.exception.DuplicateUserException;
import org.lite.gateway.exception.InvalidCredentialsException;
import org.lite.gateway.exception.ResourceNotFoundException;
import org.lite.gateway.exception.ValidationException;
import org.lite.gateway.repository.UserRepository;
import org.lite.gateway.service.JwtService;
import org.lite.gateway.service.UserService;
import org.lite.gateway.util.UserValidationUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public Mono<AuthResponse> login(LoginRequest request) {
        log.info("Login attempt for username/email: {}", request.getUsername());
        return Mono.just(request.getUsername())
            .flatMap(login -> {
                if (login.contains("@")) {
                    return userRepository.findByEmail(login);
                }
                return findByUsernameForLogin(login);
            })
            .doOnNext(user -> log.info("Found user: {}", user))
            .doOnNext(user -> {
                log.info("Raw password length: {}", request.getPassword().length());
                log.info("Raw password bytes: {}", request.getPassword().getBytes());
                StringBuilder chars = new StringBuilder("Raw password chars: ");
                for (char c : request.getPassword().toCharArray()) {
                    chars.append(String.format("[%c: %04x] ", c, (int)c));
                }
                log.info(chars.toString());
                log.info("Stored hash: {}", user.getPassword());
                log.info("New hash of literal '1234': {}", passwordEncoder.encode("1234"));
                String receivedPassword = new String(new byte[]{49, 50, 51, 52}); // Convert the exact bytes back to string
                log.info("Hash of received bytes: {}", passwordEncoder.encode(receivedPassword));
                String trimmedPassword = request.getPassword().trim();
                log.info("Trimmed password match result: {}", passwordEncoder.matches(trimmedPassword, user.getPassword()));
                log.info("Password match result: {}", passwordEncoder.matches(request.getPassword(), user.getPassword()));
            })
            .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
            .map(user -> {
                String token = jwtService.generateToken(user);
                log.info("Generated token for user: {}", user.getUsername());
                return AuthResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .message("Login successful")
                    .build();
            })
            .doOnError(error -> log.error("Login error: ", error))
            .switchIfEmpty(Mono.error(new InvalidCredentialsException()));
    }

    public Mono<AuthResponse> register(RegisterRequest request) {
        try {
            UserValidationUtil.validateRegistration(request);
        } catch (ValidationException e) {
            return Mono.error(e);
        }

        return userRepository.existsByUsername(request.getUsername())
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

                return userRepository.save(newUser)
                    .map(savedUser -> {
                        String token = jwtService.generateToken(savedUser);
                        return AuthResponse.builder()
                            .token(token)
                            .username(savedUser.getUsername())
                            .message("Registration successful")
                            .build();
                    })
                    .onErrorMap(e -> {
                        if (e.getMessage().contains("duplicate key error")) {
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
                String.format(ErrorCode.USER_NOT_FOUND.getDefaultMessage(), "username '" + username + "' does not exist")
            )))
            .doOnError(error -> log.error("Error finding user by username: {}", error.getMessage()));
    }

    @Override
    public Mono<User> save(User user) {
        return userRepository.save(user);
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
}
