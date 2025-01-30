package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.ErrorResponse;
import org.lite.gateway.dto.ErrorCode;
import org.lite.gateway.dto.KeycloakCallbackRequest;
import org.lite.gateway.service.KeycloakService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth/sso")
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthController {
    
    private final KeycloakService keycloakService;

    @PostMapping("/callback")
    public Mono<ResponseEntity<Object>> handleCallback(@RequestBody KeycloakCallbackRequest request) {
        log.info("Received SSO callback with code: {}", request.code());
        return keycloakService.handleCallback(request.code())
            .doOnSuccess(response -> log.info("SSO callback successful: {}", response))
            .doOnError(error -> log.error("SSO callback failed", error))
            .<ResponseEntity<Object>>map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.AUTHENTICATION_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    ))
            ));
    }
}