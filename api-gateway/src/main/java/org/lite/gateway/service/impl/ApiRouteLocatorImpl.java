package org.lite.gateway.service.impl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
//import org.lite.gateway.filter.CircuitBreakerGatewayFilterFactory;
import org.lite.gateway.service.RouteService;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreaker;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@RequiredArgsConstructor
@Service
@Slf4j
public class ApiRouteLocatorImpl implements RouteLocator {
    private final RouteLocatorBuilder routeLocatorBuilder;
    private final RouteService routeService;
    //private final CircuitBreakerGatewayFilterFactory circuitBreakerFilterFactory;
    private final ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory;

    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder routesBuilder = routeLocatorBuilder.routes();
        return routeService.getAll()
                .map(apiRoute -> routesBuilder.route(String.valueOf(apiRoute.getRouteIdentifier()),
                        predicateSpec -> setPredicateSpec(apiRoute, predicateSpec)))
                .collectList()
                .flatMapMany(builders -> routesBuilder.build()
                        .getRoutes());
    }

    private Buildable<Route> setPredicateSpec(ApiRoute apiRoute, PredicateSpec predicateSpec) {
        BooleanSpec booleanSpec = predicateSpec.path(apiRoute.getPath());

        if (StringUtils.hasLength(apiRoute.getMethod())) {
            booleanSpec.and().method(apiRoute.getMethod());
        }

        // Apply Filters (including CircuitBreaker)
        applyFilters(booleanSpec, apiRoute);

        return booleanSpec.uri(apiRoute.getUri());
    }

    private void applyFilters(BooleanSpec booleanSpec, ApiRoute apiRoute) {
        List<FilterConfig> filters = apiRoute.getFilters();
        if (filters != null && !filters.isEmpty()) {
            booleanSpec.filters(gatewayFilterSpec -> {
                for (FilterConfig filter : filters) {
                    switch (filter.getName()) {
                        case "CircuitBreaker" -> {
                            // Extract circuit breaker parameters from the filter config args
                            String cbName = filter.getArgs().get("name");
                            int slidingWindowSize = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("slidingWindowSize")));
                            float failureRateThreshold = Float.parseFloat(Objects.requireNonNull(filter.getArgs().get("failureRateThreshold")));
                            Duration waitDurationInOpenState = Duration.parse(filter.getArgs().get("waitDurationInOpenState"));
                            int permittedCallsInHalfOpenState = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("permittedNumberOfCallsInHalfOpenState")));
                            String fallbackUri = filter.getArgs().get("fallbackUri");
                            String recordFailurePredicate = filter.getArgs().get("recordFailurePredicate"); // Handle recordFailurePredicate if provided
                            boolean automaticTransition = Boolean.parseBoolean(Objects.requireNonNull(filter.getArgs().get("automaticTransitionFromOpenToHalfOpenEnabled")));

                            // Create the CircuitBreaker config based on the data from mongodb
                            CircuitBreakerConfig.Builder cbConfigBuilder = CircuitBreakerConfig.custom()
                                    .slidingWindowSize(slidingWindowSize)
                                    .failureRateThreshold(failureRateThreshold)
                                    .waitDurationInOpenState(waitDurationInOpenState)
                                    .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpenState)
                                    .automaticTransitionFromOpenToHalfOpenEnabled(automaticTransition);

                            // Apply custom failure predicate (e.g., HttpResponsePredicate)
                            if ("HttpResponsePredicate".equals(recordFailurePredicate)) {
                                cbConfigBuilder.recordException((throwable ->{
                                    // Check for any Exception (this can be made more granular)
                                    if (throwable instanceof Exception) {
                                        // Check for Http status 5xx-like errors or specific exception types
                                        if (throwable.getMessage().contains("5xx") ||
                                                throwable.getMessage().contains("Internal Server Error") ||
                                                throwable instanceof TimeoutException) {
                                            return true; // Record this exception as a failure
                                        }
                                    }
                                    return false;
                                }));
                            } else {
                                log.info("No valid recordFailurePredicate provided. Using default failure recording behavior.");
                            }
                            // Build custom CircuitBreakerConfig
                            CircuitBreakerConfig circuitBreakerConfig = cbConfigBuilder.build();

                            log.info("Configuring CircuitBreaker for route: {}, slidingWindowSize: {}, failureRateThreshold: {}",
                                    apiRoute.getRouteIdentifier(), slidingWindowSize, failureRateThreshold);


                            // Apply the custom CircuitBreakerConfig to the circuit breaker factory
                            reactiveResilience4JCircuitBreakerFactory.configure(builder -> builder.circuitBreakerConfig(circuitBreakerConfig), cbName);
                            ReactiveCircuitBreaker circuitBreaker = reactiveResilience4JCircuitBreakerFactory.create(cbName);

                            // Apply the circuit breaker to the route
                            gatewayFilterSpec.filter((exchange, chain) -> {
                                return circuitBreaker.run(chain.filter(exchange), throwable -> {
                                    log.error("Circuit breaker triggered for {}: {}", apiRoute.getRouteIdentifier(), throwable.getMessage());
                                    if (fallbackUri != null) {
                                        log.info("Redirecting to fallback URI: {}", fallbackUri);
                                        exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
                                        exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUri));
                                        return exchange.getResponse().setComplete();
                                    }
                                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                                    return Mono.empty();
                                });
                            });
                        }
                        case "RateLimiter" -> {
                            // Placeholder for RateLimiter logic (can be added in the future)
                        }
                        // Add more filters as needed
                    }
                }
                return gatewayFilterSpec;
            });
        }
    }


//    private void applyFilters(BooleanSpec booleanSpec, List<FilterConfig> filters) {
//        if (filters != null && !filters.isEmpty()) {
//            booleanSpec.filters(gatewayFilterSpec -> {
//                for (FilterConfig filter : filters) {
//                    switch (filter.getName()) {
//                        case "CircuitBreaker" -> {
//                            // Create CircuitBreaker config and apply it
//                            CircuitBreakerGatewayFilterFactory.Config config =
//                                    CircuitBreakerGatewayFilterFactory.Config.builder()
//                                            .name(filter.getArgs().get("name"))
//                                            .slidingWindowSize(filter.getArgs().get("slidingWindowSize"))
//                                            .failureRateThreshold(filter.getArgs().get("failureRateThreshold"))
//                                            .waitDurationInOpenState(filter.getArgs().get("waitDurationInOpenState"))
//                                            .permittedNumberOfCallsInHalfOpenState(filter.getArgs().get("permittedNumberOfCallsInHalfOpenState"))
//                                            .fallbackUri(filter.getArgs().get("fallbackUri"))
//                                            .build();
//                            GatewayFilter circuitBreakerGatewayFilter = circuitBreakerFilterFactory.apply(config);
//                            gatewayFilterSpec.filter(circuitBreakerGatewayFilter);
//                        }
//                        case "RateLimiter" -> {
//                            // Placeholder for future RateLimiter logic
//                            // Example: gatewayFilterSpec.filter(rateLimiterFilterFactory.apply(rateLimiterConfig));
//                        }
//                        // Add more cases for future filters
//                    }
//                }
//                return gatewayFilterSpec;
//            });
//        }
//    }

    private Customizer<ReactiveResilience4JCircuitBreakerFactory> customizer(FilterConfig filter){
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(Integer.parseInt(filter.getArgs().get("slidingWindowSize")))
                        .failureRateThreshold(Float.parseFloat(filter.getArgs().get("failureRateThreshold")))
                        .waitDurationInOpenState(Duration.parse(filter.getArgs().get("waitDurationInOpenState")))
                        .permittedNumberOfCallsInHalfOpenState(Integer.parseInt(filter.getArgs().get("permittedNumberOfCallsInHalfOpenState")))
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(1))
                        .build())

                .build()
        );
    }

    @Override
    public Flux<Route> getRoutesByMetadata(Map<String, Object> metadata) {
        return RouteLocator.super.getRoutesByMetadata(metadata);
    }
}
