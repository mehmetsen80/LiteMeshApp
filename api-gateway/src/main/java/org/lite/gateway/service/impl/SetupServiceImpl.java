package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.service.SetupService;
import org.lite.gateway.dto.InitialSetupRequest;
import org.lite.gateway.entity.User;
import org.lite.gateway.exception.ValidationException;
import org.lite.gateway.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupServiceImpl implements SetupService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Boolean> isSystemInitialized() {
        return userRepository.findByRoles("SUPER_ADMIN")
            .hasElements()
            .doOnSuccess(exists -> {
                if (exists) {
                    log.info("System check: Super admin exists");
                } else {
                    log.info("System check: No super admin found");
                }
            });
    }

    @Override
    public Mono<User> createInitialSuperAdmin(InitialSetupRequest request) {
        return isSystemInitialized()
            .flatMap(initialized -> {
                if (initialized) {
                    return Mono.error(new IllegalStateException("System is already initialized"));
                }

                // Validate request
                if (!isValidSetupRequest(request)) {
                    return Mono.error(new ValidationException("Invalid setup request"));
                }

                // Create super admin user
                User superAdmin = new User();
                superAdmin.setUsername(request.getUsername());
                superAdmin.setEmail(request.getEmail());
                superAdmin.setPassword(passwordEncoder.encode(request.getPassword()));
                superAdmin.setActive(true);
                superAdmin.setRoles(Set.of("SUPER_ADMIN"));

                return userRepository.save(superAdmin)
                    .doOnSuccess(user -> log.info("Super admin created successfully: {}", user.getUsername()))
                    .doOnError(error -> log.error("Failed to create super admin: {}", error.getMessage()));
            });
    }

    private boolean isValidSetupRequest(InitialSetupRequest request) {
        return request.getUsername() != null && !request.getUsername().isEmpty() &&
               request.getEmail() != null && !request.getEmail().isEmpty() &&
               request.getPassword() != null && request.getPassword().length() >= 8 &&
               request.getPassword().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$");
    }
} 