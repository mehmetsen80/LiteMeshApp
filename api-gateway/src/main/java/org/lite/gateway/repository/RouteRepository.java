package org.lite.gateway.repository;

import org.lite.gateway.entity.ApiRoute;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends ReactiveCrudRepository<ApiRoute, String> {
}
