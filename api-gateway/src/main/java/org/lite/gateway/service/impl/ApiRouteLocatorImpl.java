package org.lite.gateway.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.*;
import org.lite.gateway.filter.circuitbreaker.CircuitBreakerFilterStrategy;
import org.lite.gateway.filter.ratelimiter.RedisRateLimiterFilterStrategy;
import org.lite.gateway.filter.RetryFilter;
import org.lite.gateway.filter.timelimiter.TimeLimiterFilterStrategy;
import org.lite.gateway.model.CircuitBreakerRecord;
import org.lite.gateway.model.RedisRateLimiterRecord;
import org.lite.gateway.model.RetryRecord;
import org.lite.gateway.model.TimeLimiterRecord;
import org.lite.gateway.service.RouteService;
import org.springframework.beans.BeansException;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@RequiredArgsConstructor
@Service
@Slf4j
public class ApiRouteLocatorImpl implements RouteLocator, ApplicationContextAware {
    private final RouteLocatorBuilder routeLocatorBuilder;
    private final RouteService routeService;
    private final ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory;
//    private final CustomRetryResponseFilter customRetryResponseFilter;

    private ApplicationContext applicationContext;

    // Map for filter strategies
    private final Map<String, FilterStrategy> filterStrategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // Register filter strategies
        filterStrategyMap.put("CircuitBreaker", new CircuitBreakerFilterStrategy(reactiveResilience4JCircuitBreakerFactory));
        filterStrategyMap.put("RedisRateLimiter", new RedisRateLimiterFilterStrategy(applicationContext));
        filterStrategyMap.put("TimeLimiter", new TimeLimiterFilterStrategy());
        //filterStrategyMap.put("Retry", new RetryFilterStrategyOld());

        // Add more strategies here as needed
    }

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
                    FilterStrategy strategy = filterStrategyMap.get(filter.getName());

//                    if (strategy != null) {
//                        strategy.apply(apiRoute, gatewayFilterSpec, filter);
//                    } else {
//                        log.warn("No strategy found for filter: {}", filter.getName());
//                    }
                    String routeId = apiRoute.getRouteIdentifier();
                    switch (filter.getName()) {
                        case "CircuitBreaker" -> {
                            String cbName = filter.getArgs().get("name");
                            int slidingWindowSize = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("slidingWindowSize")));
                            float failureRateThreshold = Float.parseFloat(Objects.requireNonNull(filter.getArgs().get("failureRateThreshold")));
                            Duration waitDurationInOpenState = Duration.parse(filter.getArgs().get("waitDurationInOpenState"));
                            int permittedCallsInHalfOpenState = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("permittedNumberOfCallsInHalfOpenState")));
                            String fallbackUriCircuitBreaker = filter.getArgs().get("fallbackUri");
                            String recordFailurePredicate = filter.getArgs().get("recordFailurePredicate"); // Handle recordFailurePredicate if provided
                            boolean automaticTransition = Boolean.parseBoolean(Objects.requireNonNull(filter.getArgs().get("automaticTransitionFromOpenToHalfOpenEnabled")));
                            CircuitBreakerRecord circuitBreakerRecord = new CircuitBreakerRecord(routeId, cbName, slidingWindowSize, failureRateThreshold, waitDurationInOpenState, permittedCallsInHalfOpenState, fallbackUriCircuitBreaker, recordFailurePredicate, automaticTransition);
                            gatewayFilterSpec.filter(new CircuitBreakerFilter(reactiveResilience4JCircuitBreakerFactory, circuitBreakerRecord));
                        }
                        case "RedisRateLimiter" -> {
                            int replenishRate = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("replenishRate")));
                            int burstCapacity = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("burstCapacity")));
                            int requestedTokens = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("requestedTokens")));
                            RedisRateLimiterRecord redisRateLimiterRecord = new RedisRateLimiterRecord(routeId, replenishRate, burstCapacity, requestedTokens);
                            RedisRateLimiter redisRateLimiter = new RedisRateLimiter(redisRateLimiterRecord.replenishRate(), redisRateLimiterRecord.burstCapacity(), redisRateLimiterRecord.requestedTokens());
                            redisRateLimiter.setApplicationContext(applicationContext);
                            gatewayFilterSpec.requestRateLimiter().configure(config -> {
                                config.setRouteId(apiRoute.getRouteIdentifier());
                                config.setRateLimiter(redisRateLimiter); // Set the RedisRateLimiter
                                //config.setKeyResolver(exchange -> Mono.just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress())); // Use IP address as the key for limiting
                                config.setKeyResolver(exchange -> Mono.just(Objects.requireNonNull(apiRoute.getRouteIdentifier())));
                                config.setDenyEmptyKey(true); // Deny requests that have no resolved key
                                config.setEmptyKeyStatus(HttpStatus.TOO_MANY_REQUESTS.name()); // Set response status to 429 when no key is resolved
                            }).filter(new RedisRateLimiterFilter(redisRateLimiterRecord));
                        }
                        case "TimeLimiter" -> {
                            int timeoutDuration = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("timeoutDuration")));
                            boolean cancelRunningFuture = Boolean.parseBoolean(Objects.requireNonNull(filter.getArgs().get("cancelRunningFuture")));
                            TimeLimiterRecord timeLimiterRecord = new TimeLimiterRecord(routeId, timeoutDuration, cancelRunningFuture);
                            gatewayFilterSpec.filter(new TimeLimiterFilter(timeLimiterRecord));
                        }
                        case "Retry" -> {
                            int maxAttempts = Integer.parseInt(filter.getArgs().get("maxAttempts"));
                            Duration waitDuration = Duration.parse(filter.getArgs().get("waitDuration"));
                            String retryExceptions = filter.getArgs().get("retryExceptions"); // Optional
                            String fallbackUriRetry = filter.getArgs().get("fallbackUri");
                            RetryRecord retryRecord = new RetryRecord(routeId, maxAttempts, waitDuration, retryExceptions, fallbackUriRetry);
                            gatewayFilterSpec.filter(new RetryFilter(retryRecord));
                        }
                        default -> log.warn("No strategy found for filter: {}", filter.getName());
                    }

//                    if(filter.getName().equals("CircuitBreaker")){
//                        String routeId = apiRoute.getRouteIdentifier();
//                        String cbName = filter.getArgs().get("name");
//                        int slidingWindowSize = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("slidingWindowSize")));
//                        float failureRateThreshold = Float.parseFloat(Objects.requireNonNull(filter.getArgs().get("failureRateThreshold")));
//                        Duration waitDurationInOpenState = Duration.parse(filter.getArgs().get("waitDurationInOpenState"));
//                        int permittedCallsInHalfOpenState = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("permittedNumberOfCallsInHalfOpenState")));
//                        String fallbackUri = filter.getArgs().get("fallbackUri");
//                        String recordFailurePredicate = filter.getArgs().get("recordFailurePredicate"); // Handle recordFailurePredicate if provided
//                        boolean automaticTransition = Boolean.parseBoolean(Objects.requireNonNull(filter.getArgs().get("automaticTransitionFromOpenToHalfOpenEnabled")));
//                        CircuitBreakerRecord circuitBreakerRecord  = new CircuitBreakerRecord(routeId, cbName, slidingWindowSize, failureRateThreshold, waitDurationInOpenState, permittedCallsInHalfOpenState, fallbackUri, recordFailurePredicate, automaticTransition);
//                        gatewayFilterSpec.filter(new CircuitBreakerFilter(reactiveResilience4JCircuitBreakerFactory, circuitBreakerRecord));
//
//                    }
//                    else if(filter.getName().equals("Retry")){
//                        String routeId = apiRoute.getRouteIdentifier();
//                        int maxAttempts = Integer.parseInt(filter.getArgs().get("maxAttempts"));
//                        Duration waitDuration = Duration.parse(filter.getArgs().get("waitDuration"));
//                        String retryExceptions = filter.getArgs().get("retryExceptions"); // Optional
//                        String fallbackUri = filter.getArgs().get("fallbackUri");
//                        RetryRecord retryRecord = new RetryRecord(routeId, maxAttempts, waitDuration, retryExceptions, fallbackUri);
//                        gatewayFilterSpec.filter(new RetryFilter(retryRecord));
//                    } else {
//                        if (strategy != null) {
//                            strategy.apply(apiRoute, gatewayFilterSpec, filter);
//                        } else {
//                            log.warn("No strategy found for filter: {}", filter.getName());
//                        }
//                    }
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
