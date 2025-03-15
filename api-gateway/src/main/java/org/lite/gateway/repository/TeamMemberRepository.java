package org.lite.gateway.repository;

import org.lite.gateway.entity.TeamMember;
import org.lite.gateway.entity.TeamMemberStatus;
import org.lite.gateway.enums.UserRole;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TeamMemberRepository extends ReactiveMongoRepository<TeamMember, String> {
    Flux<TeamMember> findByUserIdAndStatus(String userId, TeamMemberStatus status);
    Flux<TeamMember> findByUserId(String userId);
    Flux<TeamMember> findByTeamId(String teamId);
    Mono<TeamMember> findByTeamIdAndUserId(String teamId, String userId);
    Mono<Void> deleteByTeamIdAndUserId(String teamId, String userId);
    Mono<Void> deleteByTeamId(String teamId);
    Mono<TeamMember> findByTeamIdAndUserIdAndRole(String teamId, String userId, UserRole role);
}
