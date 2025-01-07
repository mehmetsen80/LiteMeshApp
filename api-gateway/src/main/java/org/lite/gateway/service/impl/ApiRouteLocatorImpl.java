package org.lite.gateway.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiMetric;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.service.*;
import org.springframework.beans.BeansException;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
    private final RedisTemplate<String, String> redisTemplate;
    private final MetricService metricService;

    private ApplicationContext applicationContext;
    private Map<String, FilterService> filterServiceMap;

    @PostConstruct
    public void init() {
        this.filterServiceMap = Map.of(
                "CircuitBreaker", new CircuitBreakerFilterService(reactiveResilience4JCircuitBreakerFactory),
                "RedisRateLimiter", new RedisRateLimiterFilterService(applicationContext, redisTemplate),
                "TimeLimiter", new TimeLimiterFilterService(),
                "Retry", new RetryFilterService()
        );
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

        applyFilters(booleanSpec, apiRoute);

        // Add a custom filter to capture metrics only for real requests
        booleanSpec.filters(f -> f.filter((exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            return chain.filter(exchange)
                    .then(Mono.fromRunnable(() -> {
                        long duration = System.currentTimeMillis() - startTime;
                        boolean success = Objects.requireNonNull(exchange.getResponse().getStatusCode()).is2xxSuccessful();
                        captureMetricsForExchange(apiRoute, exchange, duration, success);
                    }));
        }));

        return booleanSpec.uri(apiRoute.getUri());
    }


    private void captureMetricsForExchange(ApiRoute apiRoute, ServerWebExchange exchange, long duration, boolean success) {
        String pathEndpoint = exchange.getRequest().getURI().getPath();
        
        // Check for health endpoint BEFORE any processing
        if (pathEndpoint.endsWith("/health") || pathEndpoint.endsWith("/health/")) {
            return;
        }

        ApiMetric metric = new ApiMetric();
        metric.setRouteIdentifier(apiRoute.getRouteIdentifier());
        metric.setTimestamp(LocalDateTime.now());
        metric.setDuration(duration);
        metric.setSuccess(success);

        // Set toService
        // Check for custom service name header
        String fromService = determineInteractionType(exchange, metric);
        metric.setFromService(fromService);

        // Set toService
        String toService = extractServiceNameFromUri(apiRoute.getUri());
        metric.setToService(toService);

        // Set gatewayBaseUrl
        String gatewayBaseUrl = exchange.getRequest().getURI().getScheme() + "://" +
                exchange.getRequest().getURI().getHost() + ":" +
                exchange.getRequest().getURI().getPort();
        metric.setGatewayBaseUrl(gatewayBaseUrl);

        // Set pathEndpoint (the path part of the URL)
        metric.setPathEndPoint(pathEndpoint);

        // Set queryParameters
        // Extract queryParameters for GET requests
        if ("GET".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            String queryParameters = exchange.getRequest().getURI().getQuery();
            metric.setQueryParameters(queryParameters != null ? queryParameters : "");
        }

        // Save metrics
        if ("POST".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            exchange.getRequest().getBody().collectList().flatMap(dataBuffers -> {
                StringBuilder bodyBuilder = new StringBuilder();
                dataBuffers.forEach(buffer -> bodyBuilder.append(StandardCharsets.UTF_8.decode(buffer.toByteBuffer())));
                metric.setRequestPayload(bodyBuilder.toString());
                return metricService.saveMetric(metric);
            }).subscribe();
        } else {
            metricService.saveMetric(metric).subscribe();
        }

        // Log metrics
        log.debug("Captured Metrics - InteractionType: {}, From: {}, To: {}, Base URL: {}, Path: {}, Duration: {}ms, Success: {}",
                metric.getInteractionType(), metric.getFromService(), metric.getToService(),
                metric.getGatewayBaseUrl(), metric.getPathEndPoint(), metric.getDuration(), metric.isSuccess());
    }

    private String determineInteractionType(ServerWebExchange exchange, ApiMetric metric) {
        String fromService = exchange.getRequest().getHeaders().getFirst("X-Service-Name");
        if (fromService != null && !fromService.isEmpty()) {
            metric.setInteractionType("APP_TO_APP");
        } else {
            String remoteAddress = exchange.getRequest().getRemoteAddress() != null ?
                    exchange.getRequest().getRemoteAddress().getHostName() : "unknown";

            if ("0:0:0:0:0:0:0:1".equals(remoteAddress)) {
                remoteAddress = "127.0.0.1";
            }
            fromService = remoteAddress;
            metric.setInteractionType("USER_TO_APP");
        }
        return fromService;
    }


    private String extractServiceNameFromUri(String uri) {
        // Example URI: "lb://inventory-service" or "http://inventory-service"
        if (uri.startsWith("lb://") || uri.startsWith("http://") || uri.startsWith("https://")) {
            return uri.split("://")[1].split("/")[0]; // Extract the service name
        }
        return uri; // Fallback in case the URI has an unexpected format
    }


    private void applyFilters(BooleanSpec booleanSpec, ApiRoute apiRoute) {
        List<FilterConfig> filters = apiRoute.getFilters();
        if (filters != null && !filters.isEmpty()) {
            booleanSpec.filters(gatewayFilterSpec -> {
                for (FilterConfig filter : filters) {
                    String filterName = filter.getName();
                    FilterService filterService = filterServiceMap.get(filterName);
                    if (filterService != null) {
                        try {
                            filterService.applyFilter(gatewayFilterSpec, filter, apiRoute);
                        } catch (Exception e) {
                            log.error("Error applying filter {} for route {}: {}", filterName, apiRoute.getRouteIdentifier(), e.getMessage());
                        }
                    } else {
                        log.warn("No filter service found for filter: {}", filterName);
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
