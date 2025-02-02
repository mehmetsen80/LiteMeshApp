package org.lite.gateway.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.model.CircuitBreakerRecord;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
public class CircuitBreakerFilter implements GatewayFilter, Ordered {


    private final ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory;
    private final CircuitBreakerRecord circuitBreakerRecord;

    public CircuitBreakerFilter(ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory, CircuitBreakerRecord circuitBreakerRecord) {
        this.reactiveResilience4JCircuitBreakerFactory = reactiveResilience4JCircuitBreakerFactory;
        this.circuitBreakerRecord = circuitBreakerRecord;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // Create the CircuitBreaker config based on the data from mongodb
        CircuitBreakerConfig.Builder cbConfigBuilder = CircuitBreakerConfig.custom()
                .slidingWindowSize(circuitBreakerRecord.slidingWindowSize())
                .failureRateThreshold(circuitBreakerRecord.failureRateThreshold())
                .waitDurationInOpenState(circuitBreakerRecord.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(circuitBreakerRecord.permittedCallsInHalfOpenState())
                .recordExceptions(TimeoutException.class, NotFoundException.class, WebClientResponseException.InternalServerError.class, HttpServerErrorException.InternalServerError.class)
                .automaticTransitionFromOpenToHalfOpenEnabled(circuitBreakerRecord.automaticTransition());

        // Apply custom failure predicate (e.g., HttpResponsePredicate)
        //statusCode >= 500
        if ("HttpResponsePredicate".equals(circuitBreakerRecord.recordFailurePredicate())) {
            cbConfigBuilder.recordException(throwable -> {
                if (throwable == null) {
                    log.warn("Throwable is null in recordException");
                    return false;
                }
                
                String message = throwable.getMessage();
                log.info("inside recordException: {}", throwable.getMessage());

                // Treat TimeoutException as a failure
                if (throwable instanceof TimeoutException) {
                    log.info("Recording TimeoutException in CircuitBreaker as a failure.");
                    return true; // Record the timeout as a failure for CircuitBreaker
                }

                // Handle 429 TOO_MANY_REQUESTS response
                if (throwable instanceof WebClientResponseException responseException) {
                    if (responseException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        log.info("Recording 429 TOO_MANY_REQUESTS in CircuitBreaker.");
                        return true; // Record the 429 status as a failure
                    }
                }

                if(throwable instanceof RuntimeException runtimeException){
                    if(message != null && message.contains("429 TOO_MANY_REQUESTS")){
                        log.info("Recording  429 TOO_MANY_REQUESTS in CircuitBreaker. {}", runtimeException.getMessage());
                        return true;
                    }
                }

                // Record other exceptions as failures based on conditions
                if (throwable instanceof Exception) {
                    if (message != null &&
                            (message.contains("5xx") ||
                                    message.contains("recorded a timeout exception"))) {
                        log.info("Recording 503 Service Unavailable or 504 errors in CircuitBreaker.");
                        return true; // Short-circuit for 503 and 504 errors
                    }
                }

                return false; // Do not record other exceptions
            });
        } else {
            log.info("No valid recordFailurePredicate provided. Using default failure recording behavior.");
        }
        // Build custom CircuitBreakerConfig
        CircuitBreakerConfig circuitBreakerConfig = cbConfigBuilder.build();

        log.info("Configuring CircuitBreaker for route: {}, slidingWindowSize: {}, failureRateThreshold: {}",
                circuitBreakerRecord.routeId(), circuitBreakerRecord.slidingWindowSize(), circuitBreakerRecord.failureRateThreshold());


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
                circuitBreakerRecord.cbName());
        ReactiveCircuitBreaker circuitBreaker = reactiveResilience4JCircuitBreakerFactory.create(circuitBreakerRecord.cbName());

        // Apply the circuit breaker to the route
        return circuitBreaker.run(chain.filter(exchange), throwable -> {
            log.error("Circuit breaker triggered for {}: {}", circuitBreakerRecord.routeId(), throwable.getMessage());
            // Check if there is a fallback URI configured
            if (circuitBreakerRecord.fallbackUri() != null) {
                String exceptionMessage = throwable.getMessage() != null ?
                         URLEncoder.encode("CircuitBreaker: " + throwable.getMessage(), StandardCharsets.UTF_8) : "";

                HttpHeaders headers = exchange.getRequest().getHeaders();
                if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                    log.info("Propagating Authorization header after retries.");
                    exchange.getRequest().mutate().header(HttpHeaders.AUTHORIZATION, headers.getFirst(HttpHeaders.AUTHORIZATION));
                }

                if(exchange.getResponse().isCommitted()){
                    log.info("The response is already committed! {}", exceptionMessage);
                    return exchange.getResponse().setComplete();
                }else{
                    // Redirect to the fallback URL with the exception message, if available
                    log.info("Redirecting to fallback URI: {}", circuitBreakerRecord.fallbackUri());
                    log.info("The response has not been committed yet!");
                    String fallbackUrlWithException = circuitBreakerRecord.fallbackUri() + "?exceptionMessage=" + exceptionMessage;
                    exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
                    exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUrlWithException));
                    // Ensure the response is completed properly
                    return exchange.getResponse().setComplete();  // This sends the response
                }
            }

            // Default behavior if no fallback is configured
            log.warn("No fallback URI configured. Responding with SERVICE_UNAVAILABLE.");
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().setComplete(); // Ensure we complete the response
        });

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }
}
