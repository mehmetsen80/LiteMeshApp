//package org.lite.gateway.filter;
//
//import io.github.resilience4j.circuitbreaker.CircuitBreaker;
//import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
//import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
//import lombok.Builder;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//import reactor.core.publisher.Mono;
//
//import java.net.URI;
//import java.time.Duration;
//
///**
// *      slidingWindowSize: 5           # Use a smaller window size for quicker testing
// *      failureRateThreshold: 20        # Allow only 20% failure rate
// *      waitDurationInOpenState: 5s     # Open state for 5 seconds
// *      permittedNumberOfCallsInHalfOpenState: 1  # Allow only 1 retry in half-open state
// */
//@Service
//@Slf4j
//public class CircuitBreakerGatewayFilterFactory extends AbstractGatewayFilterFactory<CircuitBreakerGatewayFilterFactory.Config> {
//
//    private final CircuitBreakerRegistry circuitBreakerRegistry;
//
//    public CircuitBreakerGatewayFilterFactory(CircuitBreakerRegistry circuitBreakerRegistry) {
//        super(Config.class);
//        this.circuitBreakerRegistry = circuitBreakerRegistry;
//    }
//
//    @Override
//    public GatewayFilter apply(Config config) {
//
//        // Build the CircuitBreaker with a dynamic failure predicate based on the configuration
//        CircuitBreakerConfig.Builder circuitBreakerConfigBuilder = CircuitBreakerConfig.custom();
//
//        // Record failure based on HTTP response status codes
//        if ("HttpResponsePredicate".equals(config.getRecordFailurePredicate())) {
//            circuitBreakerConfigBuilder.recordException(throwable -> {
//                if (throwable instanceof WebClientResponseException e) {
//                    return e.getStatusCode().is5xxServerError(); // Trigger circuit breaker for 5xx errors
//                }
//                return false;
//            });
//        }
//
//        // Dynamically configure the circuit breaker based on MongoDB data
//        CircuitBreakerConfig dynamicConfig = circuitBreakerConfigBuilder
//                .slidingWindowSize(Integer.parseInt(config.getSlidingWindowSize()))
//                .failureRateThreshold(Float.parseFloat(config.getFailureRateThreshold()))
//                .waitDurationInOpenState(Duration.parse(config.getWaitDurationInOpenState()))
//                .permittedNumberOfCallsInHalfOpenState(Integer.parseInt(config.getPermittedNumberOfCallsInHalfOpenState()))
//                .build();
//
//
//
//        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(config.getName(), dynamicConfig);
//
//        return (exchange, chain) -> {
//            log.info("CircuitBreaker filter: Request URI -> {}", exchange.getRequest().getURI());
//
//            // Apply circuit breaker logic with enhanced error handling
//            return Mono.fromCallable(() -> {
//                        if (circuitBreaker.tryAcquirePermission()) {
//                            log.info("CircuitBreaker allowed the call.");
//                            return chain.filter(exchange);
//                        } else {
//                            log.warn("CircuitBreaker is OPEN, denying request to downstream service.");
//                            throw new CircuitBreakerOpenException("CircuitBreaker is OPEN, request denied.");
//                        }
//                    })
//                    .flatMap(chainResult -> chainResult)
//                    .doOnError(throwable -> {
//                        // Log details about the error
//                        log.error("CircuitBreaker triggered due to: {}", throwable.toString());
//                        log.error("Request URI: {}", exchange.getRequest().getURI());
//                        log.error("CircuitBreaker state: {}", circuitBreaker.getState());
//
//                        // Customize the error response
//                        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
//                        exchange.getResponse().getHeaders().add("CB-Error", throwable.getMessage());
//                    })
//                    .onErrorResume(throwable -> {
//                        log.info("Applying fallback logic due to error: {}", throwable.toString());
//
//                        // Handle fallback logic based on config
//                        if (config.getFallbackUri() != null) {
//                            log.info("Redirecting to fallback URI: {}", config.getFallbackUri());
//                            exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
//                            exchange.getResponse().getHeaders().setLocation(URI.create(config.getFallbackUri()));
//                        } else {
//                            // Send a default fallback response if no URI is provided
//                            log.warn("No fallback URI configured, returning default fallback response.");
//                            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
//                            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
//
//                            // Create a JSON response body for better clarity
//                            String fallbackBody = """
//                                                        {
//                                                            "message": "Service is temporarily unavailable due to a circuit breaker. Please try again later.",
//                                                            "status": 503
//                                                        }
//                                                    """;
//                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
//                                    .bufferFactory().wrap(fallbackBody.getBytes())));
//                        }
//
//                        // Return a completed response
//                        return exchange.getResponse().setComplete();
//                    });
//        };
//    }
//
//    @Data
//    @Builder
//    public static class Config {
//        String name;
//        String fallbackUri;
//        String slidingWindowSize;
//        String failureRateThreshold;
//        String waitDurationInOpenState;
//        String permittedNumberOfCallsInHalfOpenState;
//        String recordFailurePredicate;   // For dynamic failure predicate handling
//    }
//
//    // Custom exception to handle Circuit Breaker open scenario
//    public static class CircuitBreakerOpenException extends RuntimeException {
//        public CircuitBreakerOpenException(String message) {
//            super(message);
//        }
//    }
//}
