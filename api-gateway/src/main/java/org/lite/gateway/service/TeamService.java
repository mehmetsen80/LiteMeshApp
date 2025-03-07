package org.lite.gateway.service;

import org.lite.gateway.enums.UserRole;
import org.lite.gateway.dto.TeamDTO;
import org.lite.gateway.dto.TeamRouteDTO;
import org.lite.gateway.entity.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Set;
import org.springframework.web.server.ServerWebExchange;

public interface TeamService {
    Flux<TeamDTO> getAllTeams();
    Mono<TeamDTO> getTeamById(String username);
    Mono<TeamDTO> createTeam(Team team, String userId);
    Mono<TeamDTO> updateTeam(String id, Team team);
    Mono<Void> deleteTeam(String teamId);
    Flux<TeamDTO> searchTeams(String query);
    
    // Team Members
    Mono<TeamMember> addMemberToTeam(String teamId, String userId, UserRole role);
    Flux<TeamMember> getTeamMembers(String teamId);
    Mono<TeamMember> updateMemberRole(String teamId, String userId, UserRole newRole);
    Mono<Void> removeMemberFromTeam(String teamId, String userId);
    Mono<Boolean> isUserTeamAdmin(String teamId, String userId);
    Flux<TeamDTO> getTeamsByUserId(String userId);

    // Team Routes
    Mono<TeamRoute> assignRouteToTeam(String teamId, String routeId, String assignedBy, Set<RoutePermission> permissions);
    Flux<TeamRouteDTO> getTeamRoutes(String teamId);
    Flux<TeamRouteDTO> getAllTeamRoutes(String username);
    Mono<Boolean> hasRouteAccess(String teamId, String routeId, RoutePermission permission);
    Mono<Void> removeRouteFromTeam(String teamId, String routeId);
    Mono<TeamRoute> updateRoutePermissions(String teamId, String routeId, Set<RoutePermission> permissions);
    Mono<TeamDTO> addTeamRoute(String teamId, String routeId, String assignedBy, Set<RoutePermission> permissions);
    Mono<TeamDTO> removeTeamRoute(String teamId, String routeId);

    // Team Status
    Mono<TeamDTO> deactivateTeam(String teamId);
    Mono<TeamDTO> activateTeam(String teamId);

    // Utility
    Mono<TeamDTO> convertToDTO(Team team);

    Flux<TeamDTO> getTeamsByUsername(String username);

    Mono<Boolean> hasRole(String teamId, String username, String role);
}

