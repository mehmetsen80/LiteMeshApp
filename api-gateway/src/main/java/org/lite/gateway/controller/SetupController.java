package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.InitialSetupRequest;
import org.lite.gateway.service.SetupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
@Slf4j
public class SetupController {
    private final SetupService setupService;

    @PostMapping("/init")
    public Mono<ResponseEntity<?>> initializeSystem(@Valid @RequestBody InitialSetupRequest request) {
        return setupService.isSystemInitialized()
            .flatMap(initialized -> {
                if (initialized) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "System is already initialized");
                    return Mono.just((ResponseEntity<?>) ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(response));
                }
                return setupService.createInitialSuperAdmin(request)
                    .map(user -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "System initialized successfully");
                        response.put("username", user.getUsername());
                        return (ResponseEntity<?>) ResponseEntity.ok(response);
                    });
            })
            .onErrorResume(e -> {
                log.error("Setup failed: ", e);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", e.getMessage());
                return Mono.just((ResponseEntity<?>) ResponseEntity
                    .badRequest()
                    .body(response));
            });
    }
} 