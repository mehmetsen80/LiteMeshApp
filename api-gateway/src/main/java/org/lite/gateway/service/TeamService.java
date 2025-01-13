package org.lite.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.Team;
import org.lite.gateway.entity.TeamMember;
import org.lite.gateway.entity.TeamMemberStatus;
import org.lite.gateway.entity.TeamRole;
import org.lite.gateway.entity.TeamStatus;
import org.lite.gateway.entity.TeamRoute;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.RoutePermission;
import org.lite.gateway.repository.TeamRepository;
import org.lite.gateway.repository.TeamMemberRepository;
import org.lite.gateway.repository.TeamRouteRepository;
import org.lite.gateway.repository.ApiRouteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {
    
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRouteRepository teamRouteRepository;
    private final ApiRouteRepository apiRouteRepository;

    public Mono<Team> createTeam(Team team, String userId) {
        team.setOwnerIds(List.of(userId));
        team.setStatus(TeamStatus.ACTIVE);
        return teamRepository.save(team)
            .flatMap(savedTeam -> {
                return addMemberToTeam(savedTeam.getId(), userId, TeamRole.ADMIN)
                    .thenReturn(savedTeam);
            });
    }

    public Mono<TeamMember> addMemberToTeam(String teamId, String userId, TeamRole role) {
        return teamRepository.findById(teamId)
            .flatMap(team -> {
                TeamMember member = TeamMember.builder()
                    .teamId(teamId)
                    .userId(userId)
                    .role(role)
                    .status(TeamMemberStatus.ACTIVE)
                    .build();
                return teamMemberRepository.save(member);
            });
    }

    public Flux<Team> getTeamsByUserId(String userId) {
        return teamMemberRepository.findByUserId(userId)
            .flatMap(member -> teamRepository.findById(member.getTeamId()));
    }

    public Flux<TeamMember> getTeamMembers(String teamId) {
        return teamMemberRepository.findByTeamId(teamId);
    }

    public Mono<TeamMember> updateMemberRole(String teamId, String userId, TeamRole newRole) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .flatMap(member -> {
                member.setRole(newRole);
                return teamMemberRepository.save(member);
            });
    }

    public Mono<Void> removeMemberFromTeam(String teamId, String userId) {
        return teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);
    }

    public Mono<Void> deleteTeam(String teamId) {
        return teamRepository.deleteById(teamId)
            .then(teamMemberRepository.deleteByTeamId(teamId));
    }

    public Mono<Boolean> isUserTeamAdmin(String teamId, String userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(member -> member.getRole() == TeamRole.ADMIN)
            .defaultIfEmpty(false);
    }

    public Flux<Team> searchTeams(String query) {
        return teamRepository.findByNameContainingIgnoreCase(query);
    }

    public Mono<Boolean> isTeamOwner(String teamId, String userId) {
        return teamRepository.findById(teamId)
            .map(team -> team.getOwnerIds().contains(userId))
            .defaultIfEmpty(false);
    }

    public Mono<Team> addTeamOwner(String teamId, String newOwnerId) {
        return teamRepository.findById(teamId)
            .flatMap(team -> {
                if (!team.getOwnerIds().contains(newOwnerId)) {
                    team.getOwnerIds().add(newOwnerId);
                    return teamRepository.save(team);
                }
                return Mono.just(team);
            });
    }

    public Mono<Team> removeTeamOwner(String teamId, String ownerId) {
        return teamRepository.findById(teamId)
            .flatMap(team -> {
                if (team.getOwnerIds().size() <= 1) {
                    return Mono.error(new IllegalStateException("Cannot remove the last owner"));
                }
                team.getOwnerIds().remove(ownerId);
                return teamRepository.save(team);
            });
    }

    public Mono<Boolean> hasMultipleOwners(String teamId) {
        return teamRepository.findById(teamId)
            .map(team -> team.getOwnerIds().size() > 1)
            .defaultIfEmpty(false);
    }

    public Mono<TeamRoute> assignRouteToTeam(String teamId, String routeId, String assignedBy) {
        return teamRepository.findById(teamId)
            .flatMap(team -> apiRouteRepository.findById(routeId)
                .flatMap(route -> {
                    TeamRoute teamRoute = TeamRoute.builder()
                        .teamId(teamId)
                        .routeId(routeId)
                        .assignedAt(LocalDateTime.now())
                        .assignedBy(assignedBy)
                        .build();
                    return teamRouteRepository.save(teamRoute);
                }));
    }

    public Flux<ApiRoute> getTeamRoutes(String teamId) {
        return teamRouteRepository.findByTeamId(teamId)
            .flatMap(teamRoute -> apiRouteRepository.findById(teamRoute.getRouteId()));
    }

    public Mono<Boolean> hasRouteAccess(String teamId, String routeId, RoutePermission permission) {
        return teamRouteRepository.findByTeamIdAndRouteId(teamId, routeId)
            .map(teamRoute -> teamRoute.getPermissions().contains(permission))
            .defaultIfEmpty(false);
    }

    public Mono<Void> removeRouteFromTeam(String teamId, String routeId) {
        return teamRouteRepository.deleteByTeamIdAndRouteId(teamId, routeId);
    }

    public Mono<TeamRoute> updateRoutePermissions(String teamId, String routeId, Set<RoutePermission> permissions) {
        return teamRouteRepository.findByTeamIdAndRouteId(teamId, routeId)
            .flatMap(teamRoute -> {
                teamRoute.setPermissions(permissions);
                return teamRouteRepository.save(teamRoute);
            });
    }
}

