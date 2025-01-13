package org.lite.gateway.repository;

import org.lite.gateway.entity.Team;
import org.lite.gateway.entity.TeamStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TeamRepository extends ReactiveMongoRepository<Team, String> {
    // Basic queries
    Flux<Team> findByNameContainingIgnoreCase(String name);
    Mono<Team> findByName(String name);
    
    // Owner related queries
    Flux<Team> findByOwnerIdsContaining(String ownerId);
    Mono<Boolean> existsByOwnerIdsContaining(String ownerId);
    
    // Route related queries
    Flux<Team> findByRouteIdsContaining(String routeId);
    
    // Status related queries
    Flux<Team> findByStatus(TeamStatus status);
    
    // Combined queries
    @Query("{ 'ownerIds': ?0, 'status': ?1 }")
    Flux<Team> findByOwnerIdAndStatus(String ownerId, TeamStatus status);
    
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'status': ?1 }")
    Flux<Team> findByNameContainingAndStatus(String name, TeamStatus status);
}