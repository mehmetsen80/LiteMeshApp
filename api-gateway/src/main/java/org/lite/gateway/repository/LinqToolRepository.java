package org.lite.gateway.repository;

import org.lite.gateway.entity.LinqTool;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface LinqToolRepository extends ReactiveMongoRepository<LinqTool, String> {
    Mono<LinqTool> findByTargetAndTeam(String target, String team);
}
