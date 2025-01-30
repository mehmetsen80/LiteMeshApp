package org.lite.gateway.repository;

import org.lite.gateway.entity.Organization;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrganizationRepository extends ReactiveMongoRepository<Organization, String> {
    Mono<Organization> findByName(String name);
    Flux<Organization> findByNameContainingIgnoreCase(String name);
} 