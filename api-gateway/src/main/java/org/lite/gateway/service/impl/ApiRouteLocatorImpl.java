package org.lite.gateway.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.*;
import org.lite.gateway.filter.circuitbreaker.CircuitBreakerFilterStrategy;
import org.lite.gateway.filter.ratelimiter.RedisRateLimiterFilterStrategy;
//import org.lite.gateway.filter.retry.RetryFilterStrategy;
import org.lite.gateway.filter.retry.RetryFilterStrategyOld2;
import org.lite.gateway.filter.timelimiter.TimeLimiterFilterStrategy;
import org.lite.gateway.service.RouteService;
import org.springframework.beans.BeansException;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
        //filterStrategyMap.put("Retry", new RetryFilterStrategy());
        filterStrategyMap.put("Retry", new RetryFilterStrategyOld2());

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

//                    if(filter.getName().equals("Retry")){
//                        gatewayFilterSpec
//                                .filter(new RetryFilterStrategy(apiRoute, filter));
//                    }


                    FilterStrategy strategy = filterStrategyMap.get(filter.getName());
                    if (strategy != null) {
                            strategy.apply(apiRoute, gatewayFilterSpec, filter);
                    } else {
                        log.warn("No strategy found for filter: {}", filter.getName());
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
