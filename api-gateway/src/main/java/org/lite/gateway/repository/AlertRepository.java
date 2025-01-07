package org.lite.gateway.repository;

import org.lite.gateway.entity.Alert;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AlertRepository extends ReactiveMongoRepository<Alert, String> {
    Flux<Alert> findByRouteId(String routeId);
    Flux<Alert> findByRouteIdAndActive(String routeId, boolean active);
    Mono<Void> deleteByRouteId(String routeId);
    Flux<Alert> findByRouteIdAndMetricAndConditionAndThreshold(
        String routeId, String metric, String condition, double threshold);
    Flux<Alert> findByRouteIdAndMetricAndActive(String routeId, String metric, boolean active);
} 