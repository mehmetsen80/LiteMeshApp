package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.OrganizationDTO;
import org.lite.gateway.dto.ErrorResponse;
import org.lite.gateway.dto.ErrorCode;
import org.lite.gateway.entity.Organization;
import org.lite.gateway.exception.InvalidAuthenticationException;
import org.lite.gateway.exception.ResourceNotFoundException;
import org.lite.gateway.exception.TeamOperationException;
import org.lite.gateway.exception.OrganizationOperationException;
import org.lite.gateway.service.OrganizationService;
import org.lite.gateway.service.UserContextService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationsController {

    private final OrganizationService organizationService;
    private final UserContextService userContextService;

    @GetMapping
    public Flux<OrganizationDTO> getAllOrganizations() {
        log.debug("REST request to get all Organizations");
        return organizationService.getAllOrganizations();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrganizationDTO>> getOrganization(@PathVariable String id) {
        log.debug("REST request to get Organization : {}", id);
        return organizationService.getOrganizationById(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<?>> createOrganization(
            @Valid @RequestBody Organization organization,
            ServerWebExchange exchange) {
        log.debug("REST request to create Organization : {}", organization);
        return userContextService.getCurrentUser(exchange)
            .flatMap(username -> {
                organization.setCreatedBy(username);
                organization.setUpdatedBy(username);
                organization.setCreatedAt(LocalDateTime.now());
                organization.setUpdatedAt(LocalDateTime.now());
                return organizationService.createOrganization(organization)
                    .<ResponseEntity<?>>map(ResponseEntity::ok);
            })
            .onErrorResume(InvalidAuthenticationException.class, e -> 
                Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.UNAUTHORIZED,
                        e.getMessage(),
                        HttpStatus.UNAUTHORIZED.value()
                    )))
            )
            .onErrorResume(TeamOperationException.class, e ->
                Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.ORGANIZATION_OPERATION_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    )))
            );
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<?>> updateOrganization(
            @PathVariable String id,
            @Valid @RequestBody Organization organization,
            ServerWebExchange exchange) {
        log.debug("REST request to update Organization : {}", organization);
        return userContextService.getCurrentUser(exchange)
            .flatMap(username -> {
                organization.setUpdatedBy(username);
                organization.setUpdatedAt(LocalDateTime.now());
                return organizationService.updateOrganization(id, organization)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .defaultIfEmpty(ResponseEntity.notFound().build());
            })
            .onErrorResume(InvalidAuthenticationException.class, e -> 
                Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.UNAUTHORIZED,
                        e.getMessage(),
                        HttpStatus.UNAUTHORIZED.value()
                    )))
            )
            .onErrorResume(TeamOperationException.class, e ->
                Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.ORGANIZATION_UPDATE_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    )))
            );
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteOrganization(@PathVariable String id) {
        log.debug("REST request to delete Organization : {}", id);
        return organizationService.deleteOrganization(id)
            .then(Mono.<ResponseEntity<?>>just(ResponseEntity.ok().build()))
            .onErrorResume(OrganizationOperationException.class, e ->
                Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.ORGANIZATION_OPERATION_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    )))
            )
            .onErrorResume(ResourceNotFoundException.class, e ->
                Mono.<ResponseEntity<?>>just(ResponseEntity.notFound().build())
            );
    }

    @GetMapping("/search")
    public Flux<OrganizationDTO> searchOrganizations(@RequestParam String query) {
        log.debug("REST request to search organizations with query: {}", query);
        return organizationService.searchOrganizations(query);
    }
} 