package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.TeamDTO;
import org.lite.gateway.entity.Team;
import org.lite.gateway.entity.TeamRole;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.service.TeamService;
import org.lite.gateway.exception.TeamOperationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.lite.gateway.dto.ErrorResponse;
import org.lite.gateway.dto.ErrorCode;
import org.lite.gateway.exception.ResourceNotFoundException;
import org.lite.gateway.service.UserContextService;
import org.lite.gateway.exception.InvalidAuthenticationException;
import org.springframework.web.server.ServerWebExchange;

import jakarta.validation.Valid;

import org.lite.gateway.dto.TeamRouteRequest;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Slf4j
public class TeamsController {

    private final TeamService teamService;
    private final UserContextService userContextService;

    @GetMapping
    public Flux<TeamDTO> getAllTeams() {
        return teamService.getAllTeams()
            .onErrorResume(TeamOperationException.class, e ->
                Flux.error(new TeamOperationException(
                    ErrorResponse.fromErrorCode(
                        ErrorCode.TEAM_OPERATION_ERROR,
                        "Failed to fetch teams",
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ).getMessage()
                ))
            );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<?>> getTeam(@PathVariable String id) {
        return teamService.getTeamById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.fromErrorCode(
                    ErrorCode.TEAM_NOT_FOUND,
                    String.format("Team with id %s not found", id),
                    HttpStatus.NOT_FOUND.value()
                ))))
            .onErrorResume(TeamOperationException.class, e ->
                Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.TEAM_OPERATION_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    ))));
    }

    @PostMapping
    public Mono<ResponseEntity<?>> createTeam(
            @Valid @RequestBody Team team,
            ServerWebExchange exchange) {
        return userContextService.getCurrentUser(exchange)
            .flatMap(username -> {
                team.setCreatedBy(username);
                team.setUpdatedBy(username);
                return teamService.createTeam(team, username);
            })
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .onErrorResume(TeamOperationException.class, e -> {
                return Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.TEAM_OPERATION_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    )));
            })
            .onErrorResume(InvalidAuthenticationException.class, e -> {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.AUTHENTICATION_ERROR,
                        e.getMessage(),
                        HttpStatus.UNAUTHORIZED.value()
                    )));
            });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<?>> updateTeam(
            @PathVariable String id,
            @Valid @RequestBody Team team,
            ServerWebExchange exchange) {
        return userContextService.getCurrentUser(exchange)
            .flatMap(username -> {
                team.setUpdatedBy(username);
                return teamService.updateTeam(id, team);
            })
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .onErrorResume(TeamOperationException.class, e -> {
                log.error("Team operation error: {}", e.getMessage());
                return Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.TEAM_OPERATION_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    )));
            })
            .onErrorResume(InvalidAuthenticationException.class, e -> {
                log.error("Authentication error: {}", e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.AUTHENTICATION_ERROR,
                        e.getMessage(),
                        HttpStatus.UNAUTHORIZED.value()
                    )));
            })
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteTeam(@PathVariable String id) {
        return teamService.deleteTeam(id)
            .<ResponseEntity<?>>thenReturn(ResponseEntity.noContent().build())
            .onErrorResume(TeamOperationException.class, e -> {
                log.error("Error deleting team: {}", e.getMessage());
                return Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.TEAM_DELETE_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    )));
            });
    }

    @PutMapping("/{id}/deactivate")
    public Mono<ResponseEntity<?>> deactivateTeam(@PathVariable String id) {
        return teamService.deactivateTeam(id)
            .<ResponseEntity<?>>map(team -> ResponseEntity.ok().body(team))
            .onErrorResume(TeamOperationException.class, e ->
                Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.TEAM_DEACTIVATE_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    ))));
    }

    @PutMapping("/{id}/activate")
    public Mono<ResponseEntity<?>> activateTeam(@PathVariable String id) {
        return teamService.activateTeam(id)
            .<ResponseEntity<?>>map(team -> ResponseEntity.ok().body(team))
            .onErrorResume(TeamOperationException.class, e ->
                Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.TEAM_ACTIVATE_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    ))));
    }

    // Team Members endpoints
    @GetMapping("/{teamId}/members")
    public Mono<ResponseEntity<?>> getTeamMembers(@PathVariable String teamId) {
        return teamService.getTeamMembers(teamId)
            .collectList()
            .map(members -> {
                if (members.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ErrorResponse.fromErrorCode(
                            ErrorCode.TEAM_NOT_FOUND,
                            String.format("Team with id %s not found", teamId),
                            HttpStatus.NOT_FOUND.value()
                        ));
                }
                return ResponseEntity.ok(members);
            })
            .onErrorResume(TeamOperationException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.TEAM_OPERATION_ERROR,
                        "Failed to fetch team members",
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                    )))
            );
    }

    @PostMapping("/{teamId}/members")
    public Mono<ResponseEntity<TeamDTO>> addTeamMember(
            @PathVariable String teamId,
            @RequestParam String username,
            @RequestParam String role) {
        TeamRole teamRole;
        try {
            teamRole = TeamRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        
        return teamService.addMemberToTeam(teamId, username, teamRole)
            .then(teamService.getTeamById(teamId))
            .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public Mono<ResponseEntity<TeamDTO>> removeTeamMember(
            @PathVariable String teamId,
            @PathVariable String userId) {
        return teamService.removeMemberFromTeam(teamId, userId)
            .then(teamService.getTeamById(teamId))
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                String.format("Team with id %s not found", teamId)
            )));
    }

    // Team Routes endpoints
    @GetMapping("/{teamId}/routes")
    public Flux<ApiRoute> getTeamRoutes(@PathVariable String teamId) {
        return teamService.getTeamRoutes(teamId);
    }

    @PostMapping("/route-assignment")
    public Mono<ResponseEntity<?>> assignRouteToTeam(
            @Valid @RequestBody TeamRouteRequest request,
            ServerWebExchange exchange) {
        return userContextService.getCurrentUser(exchange)
            .flatMap(username -> teamService.addTeamRoute(
                request.getTeamId(), 
                request.getRouteId(), 
                username,
                request.getPermissions()
            ))
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .onErrorResume(TeamOperationException.class, e ->
                Mono.just(ResponseEntity.badRequest()
                    .body(ErrorResponse.fromErrorCode(
                        ErrorCode.TEAM_OPERATION_ERROR,
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value()
                    ))
                ));
    }

    @DeleteMapping("/{teamId}/routes/{routeId}")
    public Mono<ResponseEntity<TeamDTO>> removeTeamRoute(
            @PathVariable String teamId,
            @PathVariable String routeId) {
        return teamService.removeTeamRoute(teamId, routeId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public Flux<TeamDTO> searchTeams(@RequestParam String query) {
        return teamService.searchTeams(query)
            .onErrorResume(e -> Mono.error(new TeamOperationException("Failed to search teams", e)));
    }

    @GetMapping("/user/{userId}")
    public Flux<TeamDTO> getTeamsByUserId(@PathVariable String userId) {
        return teamService.getTeamsByUserId(userId)
            .onErrorResume(e -> Mono.error(new TeamOperationException(
                String.format("Failed to fetch teams for user %s", userId), e
            )));
    }
} 