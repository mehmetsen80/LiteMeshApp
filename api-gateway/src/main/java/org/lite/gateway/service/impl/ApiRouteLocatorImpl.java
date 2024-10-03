package org.lite.gateway.service.impl;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.CustomRateLimitResponseFilter;
import org.lite.gateway.service.RouteService;
import org.springframework.beans.BeansException;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@Service
@Slf4j
public class ApiRouteLocatorImpl implements RouteLocator, ApplicationContextAware {
    private final RouteLocatorBuilder routeLocatorBuilder;
    private final RouteService routeService;
    private final ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory;

    private ApplicationContext applicationContext;

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
                                                throwable.getMessage().contains("Internal Server Error")) {
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


                            //Add time limiter by default to the CircuitBreaker but never hit this because we are handling
                            //the TimeLimiter in the next block. We've given 100 seconds to prevent the CircuitBreaker and TimeLimiter timeout conflict
                            //The TimeLimiter hits first by this way, otherwise it throws "terminal signal within 1000ms in CircuitBreaker" error by default
                            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                                    .timeoutDuration(Duration.ofSeconds(100000)) // Set a big timeout duration i.e 100 seconds
                                    .cancelRunningFuture(true) // Cancel running future on timeout
                                    .build();

                            // Apply the custom CircuitBreakerConfig to the circuit breaker factory
                            reactiveResilience4JCircuitBreakerFactory.configure(builder ->
                                    builder
                                            .circuitBreakerConfig(circuitBreakerConfig)//Please do not forget that we handle the TimeLimiter later
                                            .timeLimiterConfig(timeLimiterConfig).build(),
                                    cbName);
                            ReactiveCircuitBreaker circuitBreaker = reactiveResilience4JCircuitBreakerFactory.create(cbName);

                            // Apply the circuit breaker to the route
                            gatewayFilterSpec.filter((exchange, chain) -> {
                                return circuitBreaker.run(chain.filter(exchange), throwable -> {
                                    log.error("Circuit breaker triggered for {}: {}", apiRoute.getRouteIdentifier(), throwable.getMessage());
                                    if (fallbackUri != null) {
                                        log.info("Redirecting to fallback URI: {}", fallbackUri);
                                        // Append the exception message to the fallback URL as a query parameter
                                        String fallbackUrlWithException = fallbackUri + "?exceptionMessage=" + URLEncoder.encode(throwable.getMessage(), StandardCharsets.UTF_8);
                                        exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
                                        exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUrlWithException));
                                        return exchange.getResponse().setComplete();
                                    }
                                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                                    return Mono.empty();
                                });
                            });
                        }
                        case "RedisRateLimiter" -> {
                            // Extract rate limiter parameters from the filter config args
                            int replenishRate = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("replenishRate")));
                            int burstCapacity = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("burstCapacity")));
                            int requestedTokens = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("requestedTokens")));

                            // Configure RedisRateLimiter with extracted parameters
                            RedisRateLimiter redisRateLimiter = new RedisRateLimiter(replenishRate, burstCapacity, requestedTokens);
                            redisRateLimiter.setApplicationContext(applicationContext);
                            gatewayFilterSpec.requestRateLimiter().configure(config -> {
                                config.setRouteId(apiRoute.getRouteIdentifier());
                                config.setRateLimiter(redisRateLimiter); // Set the RedisRateLimiter
                                //config.setKeyResolver(exchange -> Mono.just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress())); // Use IP address as the key for limiting
                                config.setKeyResolver(exchange -> Mono.just(Objects.requireNonNull(apiRoute.getRouteIdentifier())));
                                config.setDenyEmptyKey(true); // Deny requests that have no resolved key
                                config.setEmptyKeyStatus(HttpStatus.TOO_MANY_REQUESTS.name()); // Set response status to 429 when no key is resolved
                            }).filter(new CustomRateLimitResponseFilter());
                        }
                        case "TimeLimiter" -> {
                            // Extract TimeLimiter parameters from the filter config args
                            int timeoutDuration = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("timeoutDuration")));
                            boolean cancelRunningFuture = Boolean.parseBoolean(Objects.requireNonNull(filter.getArgs().get("cancelRunningFuture")));

                            // Create the TimeLimiterConfig
                            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                                    .timeoutDuration(Duration.ofSeconds(5)) // Set the timeout duration
                                    .cancelRunningFuture(cancelRunningFuture) // Whether to cancel running future on timeout
                                    .build();

                            log.info("Configuring TimeLimiter for route: {}, timeoutDuration: {}, cancelRunningFuture: {}",
                                    apiRoute.getRouteIdentifier(), timeoutDuration, cancelRunningFuture);

                            // Create the TimeLimiter using the Resilience4J factory
                            TimeLimiter timeLimiter = TimeLimiter.of("timeLimiter-" + apiRoute.getRouteIdentifier(), timeLimiterConfig);

                            // Apply the TimeLimiter to the route
                            gatewayFilterSpec.filter((exchange, chain) -> {
                                log.info("Applying TimeLimiter for route: {}", apiRoute.getRouteIdentifier());

                                // Convert Mono to CompletableFuture and apply the TimeLimiter
                                CompletableFuture<Void> future = Mono.from(chain.filter(exchange))
                                        .toFuture();

                                // Apply the TimeLimiter logic
                                return Mono.fromSupplier(() -> {
                                            try {
                                                return timeLimiter.executeFutureSupplier(() -> future);
                                            } catch (Exception e) {
                                                if (e instanceof TimeoutException) {
                                                    log.error("TimeoutException occurred: {}", e.getMessage());
                                                    throw new RuntimeException(new TimeoutException("Request timed out after " + timeoutDuration + " seconds"));
                                                }
                                                throw new RuntimeException(e);
                                            }
                                        })
                                        .flatMap(ignored -> Mono.empty()) // As we are dealing with `Void`, we just return an empty Mono on success
                                        .onErrorResume(throwable -> {
                                            if (throwable instanceof TimeoutException) {
                                                log.info("Timeout occurred. Applying fallback logic for {}", apiRoute.getRouteIdentifier());
                                                exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
                                                String responseMessage = "Request timed out. Please try again later.";
                                                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseMessage.getBytes(StandardCharsets.UTF_8));
                                                return exchange.getResponse().writeWith(Mono.just(buffer));
                                            }
                                            return Mono.error(throwable); // Propagate other exceptions
                                        })
                                        .then();
                            });
                        }
                        // Add more filters as needed
                    }
                }
                return gatewayFilterSpec;
            });
        }
    }

    @Override
    public Flux<Route> getRoutesByMetadata(Map<String, Object> metadata) {
        return RouteLocator.super.getRoutesByMetadata(metadata);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
