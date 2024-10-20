package org.lite.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.CircuitBreakerFilter;
import org.lite.gateway.model.CircuitBreakerRecord;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import java.time.Duration;
import java.util.Objects;

@Slf4j
public class CircuitBreakerFilterService implements FilterService{

    private final ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory;

    public CircuitBreakerFilterService(ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory) {
        this.reactiveResilience4JCircuitBreakerFactory = reactiveResilience4JCircuitBreakerFactory;
    }

    @Override
    public void applyFilter(GatewayFilterSpec gatewayFilterSpec, FilterConfig filter, ApiRoute apiRoute) {
        try {
            String cbName = filter.getArgs().get("name");
            int slidingWindowSize = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("slidingWindowSize")));
            float failureRateThreshold = Float.parseFloat(Objects.requireNonNull(filter.getArgs().get("failureRateThreshold")));
            Duration waitDurationInOpenState = Duration.parse(filter.getArgs().get("waitDurationInOpenState"));
            int permittedCallsInHalfOpenState = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("permittedNumberOfCallsInHalfOpenState")));
            String fallbackUri = filter.getArgs().get("fallbackUri");
            String recordFailurePredicate = filter.getArgs().get("recordFailurePredicate"); // Handle recordFailurePredicate if provided
            boolean automaticTransition = Boolean.parseBoolean(Objects.requireNonNull(filter.getArgs().get("automaticTransitionFromOpenToHalfOpenEnabled")));

            CircuitBreakerRecord circuitBreakerRecord = new CircuitBreakerRecord(
                    apiRoute.getRouteIdentifier(), cbName, slidingWindowSize, failureRateThreshold, waitDurationInOpenState,
                    permittedCallsInHalfOpenState, fallbackUri, recordFailurePredicate, automaticTransition
            );

            gatewayFilterSpec.filter(new CircuitBreakerFilter(reactiveResilience4JCircuitBreakerFactory, circuitBreakerRecord));
        } catch (Exception e) {
            log.error("Error applying CircuitBreaker filter for route {}: {}", apiRoute.getRouteIdentifier(), e.getMessage());
        }
    }
}
