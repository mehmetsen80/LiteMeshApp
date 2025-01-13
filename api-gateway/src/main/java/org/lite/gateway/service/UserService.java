package org.lite.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.LoginRequest;
import org.lite.gateway.dto.RegisterRequest;
import org.lite.gateway.dto.AuthResponse;
import org.lite.gateway.entity.User;
import org.lite.gateway.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.lite.gateway.exception.DuplicateUserException;
import org.lite.gateway.exception.InvalidCredentialsException;
import org.lite.gateway.exception.ValidationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
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
                return userRepository.findByUsername(login);
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
        // Validate input
        if (request.getPassword().length() < 6) {
            return Mono.error(new ValidationException("Password must be at least 6 characters long"));
        }
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return Mono.error(new ValidationException("Invalid email format"));
        }
        if (request.getUsername().length() < 3) {
            return Mono.error(new ValidationException("Username must be at least 3 characters long"));
        }

        return userRepository.existsByUsername(request.getUsername())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new DuplicateUserException("Username already exists"));
                }

                return userRepository.existsByEmail(request.getEmail());
            })
            .flatMap(emailExists -> {
                if (emailExists) {
                    return Mono.error(new DuplicateUserException("Email already exists"));
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
                                return new DuplicateUserException("Email already exists");
                            }
                            if (e.getMessage().contains("username_1")) {
                                return new DuplicateUserException("Username already exists");
                            }
                        }
                        return e;
                    });
            });
    }
} 