package org.lite.gateway.repository;

import org.lite.gateway.entity.TeamRoute;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TeamRouteRepository extends ReactiveMongoRepository<TeamRoute, String> {
    Flux<TeamRoute> findByTeamId(String teamId);
    Mono<TeamRoute> findByTeamIdAndRouteId(String teamId, String routeId);
    Mono<Void> deleteByTeamIdAndRouteId(String teamId, String routeId);
} 