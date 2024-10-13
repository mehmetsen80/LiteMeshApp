package org.lite.gateway.filter.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.FilterStrategy;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

@Slf4j
public class CircuitBreakerFilterStrategy implements FilterStrategy {

    private final ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory;

    public CircuitBreakerFilterStrategy(ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory) {
        this.reactiveResilience4JCircuitBreakerFactory = circuitBreakerFactory;
    }

    @Override
    public GatewayFilterSpec apply(ApiRoute apiRoute, GatewayFilterSpec gatewayFilterSpec, FilterConfig filter) {

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
                    if (throwable.getMessage() != null && (throwable.getMessage().contains("5xx") ||
                            throwable.getMessage().contains("Internal Server Error"))) {
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
        return gatewayFilterSpec.filter((exchange, chain) -> {
            return circuitBreaker.run(chain.filter(exchange), throwable -> {
                log.error("Circuit breaker triggered for {}: {}", apiRoute.getRouteIdentifier(), throwable.getMessage());
                // Check if there is a fallback URI configured
                if (fallbackUri != null) {
                    log.info("Redirecting to fallback URI: {}", fallbackUri);

                    // Redirect to the fallback URL with the exception message, if available
                    String exceptionMessage = throwable.getMessage() != null ?
                            URLEncoder.encode(throwable.getMessage(), StandardCharsets.UTF_8) : "";
                    String fallbackUrlWithException = fallbackUri + "?exceptionMessage=" + exceptionMessage;

                    exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
                    exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUrlWithException));

                    // Ensure the response is completed properly
                    return exchange.getResponse().setComplete();  // This sends the response
                    //return Mono.empty();
                }

                // Default behavior if no fallback is configured
                log.warn("No fallback URI configured. Responding with SERVICE_UNAVAILABLE.");
                exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                return exchange.getResponse().setComplete(); // Ensure we complete the response
                //return Mono.empty();
            });
        });
    }
}
