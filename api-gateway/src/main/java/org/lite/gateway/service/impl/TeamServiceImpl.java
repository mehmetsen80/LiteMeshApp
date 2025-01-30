package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.lite.gateway.dto.OrganizationDTO;
import org.lite.gateway.dto.TeamDTO;
import org.lite.gateway.dto.TeamMemberDTO;
import org.lite.gateway.dto.TeamRouteDTO;
import org.lite.gateway.entity.*;
import org.lite.gateway.exception.ResourceNotFoundException;
import org.lite.gateway.exception.TeamOperationException;
import org.lite.gateway.repository.*;
import org.lite.gateway.service.TeamService;
import org.lite.gateway.service.UserService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl implements TeamService {
    
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRouteRepository teamRouteRepository;
    private final ApiRouteRepository apiRouteRepository;
    private final UserService userService;
    private final OrganizationRepository organizationRepository;

    public Mono<TeamDTO> convertToDTO(Team team) {
        Mono<List<TeamMemberDTO>> membersListMono = teamMemberRepository
            .findByTeamId(team.getId())
            .flatMap(member -> userService.findById(member.getUserId())
                .map(user -> convertToMemberDTO(member, user)))
            .collectList();
            
        Mono<List<TeamRouteDTO>> routesListMono = teamRouteRepository
            .findByTeamId(team.getId())
            .flatMap(teamRoute -> apiRouteRepository.findById(teamRoute.getRouteId())
                .map(route -> convertToRouteDTO(teamRoute, route)))
            .collectList();

        Mono<OrganizationDTO> organizationMono = Mono.justOrEmpty(team.getOrganizationId())
            .flatMap(organizationRepository::findById)
            .map(org -> OrganizationDTO.builder()
                .id(org.getId())
                .name(org.getName())
                .description(org.getDescription())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .createdBy(org.getCreatedBy())
                .updatedBy(org.getUpdatedBy())
                .build()
            );

        return Mono.zip(membersListMono, routesListMono, organizationMono)
            .map(tuple -> TeamDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .status(team.getStatus())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .createdBy(team.getCreatedBy())
                .updatedBy(team.getUpdatedBy())
                .members(tuple.getT1())
                .routes(tuple.getT2())
                .organization(tuple.getT3())
                .build()
            );
    }

    private TeamMemberDTO convertToMemberDTO(TeamMember member, User user) {
        return TeamMemberDTO.builder()
            .id(member.getId())
            .teamId(member.getTeamId())
            .userId(member.getUserId())
            .username(user.getUsername())
            .role(member.getRole())
            .status(member.getStatus())
            .joinedAt(member.getJoinedAt())
            .lastActiveAt(member.getLastActiveAt())
            .build();
    }

    private TeamRouteDTO convertToRouteDTO(TeamRoute teamRoute, ApiRoute route) {
        return TeamRouteDTO.builder()
            .id(teamRoute.getId())
            .teamId(teamRoute.getTeamId())
            .routeId(teamRoute.getRouteId())
            .routeIdentifier(route.getRouteIdentifier())
            .path(route.getPath())
            .version(route.getVersion())
            .permissions(teamRoute.getPermissions())
            .assignedAt(teamRoute.getAssignedAt())
            .assignedBy(teamRoute.getAssignedBy())
            .build();
    }

    public Mono<TeamDTO> getTeamById(String id) {
        return teamRepository.findById(id)
            .flatMap(this::convertToDTO);
    }

    @Override
    public Mono<TeamDTO> createTeam(Team team, String username) {
        log.debug("Creating team with username: {}", username);
        team.setCreatedBy(username);
        team.setUpdatedBy(username);
        team.setStatus(TeamStatus.ACTIVE);
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());
        
        return userService.findByUsername(username)
            .switchIfEmpty(Mono.error(new TeamOperationException("User not found")))
            .flatMap(user -> 
                teamRepository.save(team)
                    .flatMap(savedTeam -> 
                        addMemberToTeam(savedTeam.getId(), user.getUsername(), TeamRole.ADMIN)
                            .then(convertToDTO(savedTeam))
                    )
            )
            .doOnNext(dto -> log.debug("Created team DTO: {}", dto));
    }

    public Mono<TeamMember> addMemberToTeam(String teamId, String username, TeamRole role) {
        return teamRepository.findById(teamId)
            .switchIfEmpty(Mono.error(new TeamOperationException("Team not found")))
            .flatMap(team -> userService.findByUsername(username)
                .switchIfEmpty(Mono.error(new TeamOperationException(
                    String.format("User '%s' does not exist", username)
                )))
                .flatMap(user -> {
                    // Check if member already exists
                    return teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                        .hasElement()
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new TeamOperationException("User is already a member of this team"));
                            }
                            TeamMember member = TeamMember.builder()
                                .teamId(team.getId())
                                .userId(user.getId())
                                .role(role)
                                .status(TeamMemberStatus.ACTIVE)
                                .build();
                            return teamMemberRepository.save(member);
                        });
                }));
    }

    @Override
    public Flux<TeamDTO> getTeamsByUserId(String userId) {
        return teamMemberRepository.findByUserId(userId)
            .flatMap(member -> teamRepository.findById(member.getTeamId()))
            .flatMap(this::convertToDTO);
    }

    public Flux<TeamMember> getTeamMembers(String teamId) {
        return teamMemberRepository.findByTeamId(teamId)
            .flatMap(member -> userService.findById(member.getUserId())
                .map(user -> {
                    member.setUsername(user.getUsername());
                    return member;
                }));
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
        log.debug("Attempting to delete team with id: {}", teamId);
        return teamRepository.findById(teamId)
            .flatMap(team -> {
                if (team.getStatus() == TeamStatus.ACTIVE) {
                    log.warn("Cannot delete active team: {}", teamId);
                    return Mono.error(new TeamOperationException(
                        "Cannot delete an active team. Please deactivate it first."
                    ));
                }
                
                return Mono.zip(
                    teamMemberRepository.findByTeamId(teamId).collectList(),
                    teamRouteRepository.findByTeamId(teamId).collectList()
                ).flatMap(tuple -> {
                    List<TeamMember> members = tuple.getT1();
                    List<TeamRoute> routes = tuple.getT2();
                    
                    if (!routes.isEmpty()) {
                        log.warn("Cannot delete team {} with {} routes", teamId, routes.size());
                        return Mono.error(new TeamOperationException(
                            "Cannot delete team with assigned routes. Please remove all routes first."
                        ));
                    }
                    
                    log.debug("Deleting team members for team: {}", teamId);
                    return teamMemberRepository.deleteByTeamId(teamId)
                        .doOnSuccess(v -> log.debug("Successfully deleted team members"))
                        .then(teamRepository.deleteById(teamId))
                        .doOnSuccess(v -> log.info("Successfully deleted team: {}", teamId));
                });
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.warn("Team not found for deletion: {}", teamId);
                // If team doesn't exist, consider it as already deleted
                return Mono.empty();
            }));
    }

    public Mono<Boolean> isUserTeamAdmin(String teamId, String userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(member -> member.getRole() == TeamRole.ADMIN)
            .defaultIfEmpty(false);
    }

    public Flux<TeamDTO> searchTeams(String query) {
        return teamRepository.findByNameContainingIgnoreCase(query)
            .flatMap(this::convertToDTO);
    }

    public Mono<TeamRoute> assignRouteToTeam(String teamId, String routeId, String assignedBy, Set<RoutePermission> permissions) {
        return teamRepository.findById(teamId)
            .switchIfEmpty(Mono.error(new TeamOperationException("Team not found")))
            .flatMap(team -> apiRouteRepository.findByRouteIdentifier(routeId)
                .switchIfEmpty(Mono.error(new TeamOperationException(
                    String.format("Route '%s' does not exist", routeId)
                )))
                .flatMap(route -> teamRouteRepository.findByTeamIdAndRouteId(teamId, route.getId())
                    .hasElement()
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new TeamOperationException(
                                String.format("Route '%s' (v%d) is already assigned to this team", 
                                    route.getRouteIdentifier(), route.getVersion())
                            ));
                        }
                        TeamRoute teamRoute = TeamRoute.builder()
                            .teamId(teamId)
                            .routeId(route.getId())
                            .assignedAt(LocalDateTime.now())
                            .assignedBy(assignedBy)
                            .permissions(permissions)
                            .build();
                        return teamRouteRepository.save(teamRoute);
                    })
                ));
    }

    public Flux<ApiRoute> getTeamRoutes(String teamId) {
        return teamRouteRepository.findByTeamId(teamId)
            .flatMap(teamRoute -> 
                apiRouteRepository.findById(teamRoute.getRouteId())
                    .map(route -> {
                        // Enrich the route with permissions from TeamRoute
                        route.setPermissions(teamRoute.getPermissions());
                        return route;
                    })
            );
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

    @Override
    public Flux<TeamDTO> getAllTeams() {
        return teamRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"))
            .flatMap(this::convertToDTO);
    }

    @Override
    public Mono<TeamDTO> addTeamRoute(String teamId, String routeId, String assignedBy, Set<RoutePermission> permissions) {
        return assignRouteToTeam(teamId, routeId, assignedBy, permissions)
            .then(teamRepository.findById(teamId))
            .flatMap(this::convertToDTO);
    }

    @Override
    public Mono<TeamDTO> removeTeamRoute(String teamId, String routeId) {
        return removeRouteFromTeam(teamId, routeId)
            .then(teamRepository.findById(teamId))
            .flatMap(this::convertToDTO);
    }

    @Override
    public Mono<TeamDTO> deactivateTeam(String teamId) {
        return teamRepository.findById(teamId)
            .flatMap(team -> {
                team.setStatus(TeamStatus.INACTIVE);
                team.setUpdatedAt(LocalDateTime.now());
                return teamRepository.save(team);
            })
            .flatMap(this::convertToDTO)
            .switchIfEmpty(Mono.error(new TeamOperationException("Team not found")));
    }

    @Override
    public Mono<TeamDTO> activateTeam(String teamId) {
        return teamRepository.findById(teamId)
            .flatMap(team -> {
                team.setStatus(TeamStatus.ACTIVE);
                team.setUpdatedAt(LocalDateTime.now());
                return teamRepository.save(team);
            })
            .flatMap(this::convertToDTO)
            .switchIfEmpty(Mono.error(new TeamOperationException("Team not found")));
    }

    @Override
    public Mono<TeamDTO> updateTeam(String id, Team team) {
        return teamRepository.findById(id)
            .flatMap(existingTeam -> {
                existingTeam.setName(team.getName());
                existingTeam.setDescription(team.getDescription());
                existingTeam.setOrganizationId(team.getOrganizationId());
                existingTeam.setUpdatedAt(LocalDateTime.now());
                existingTeam.setUpdatedBy(team.getUpdatedBy());
                return teamRepository.save(existingTeam);
            })
            .flatMap(this::convertToDTO)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                String.format("Team with id %s not found", id)
            )));
    }
} 