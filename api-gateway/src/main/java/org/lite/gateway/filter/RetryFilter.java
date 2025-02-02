package org.lite.gateway.filter;

import io.github.resilience4j.retry.RetryConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.model.RetryRecord;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

@Slf4j
@Data
public class RetryFilter implements GatewayFilter, Ordered {

    RetryRecord retryRecord;

    public RetryFilter(RetryRecord retryRecord) {
        this.retryRecord = retryRecord;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        try {
            String routeId = retryRecord.routeId();
            int maxAttempts = retryRecord.maxAttempts();
            Duration waitDuration = retryRecord.waitDuration();
            String retryExceptions = retryRecord.retryExceptions();
            //log.info("Applying Retry for route: {}", routeId);

            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(maxAttempts)
                    .waitDuration(waitDuration)
                    .retryOnException(throwable -> shouldRetryOnException(throwable, retryExceptions))
                    .ignoreExceptions(TimeoutException.class, NotFoundException.class, WebClientResponseException.TooManyRequests.class) // Do not retry on TimeoutException or NotFoundException
                    .build();
            RetryBackoffSpec retryBackoffSpec = buildRetryBackoffSpec(maxAttempts, waitDuration, retryConfig);
            BodyCaptureRequest requestDecorator = new BodyCaptureRequest(exchange.getRequest());
            BodyCaptureResponse responseDecorator = new BodyCaptureResponse(exchange.getResponse());

            // We wrap the entire downstream call (chain.filter) inside the retry logic
            // We use Mono.defer to make sure that each retry triggers a fresh request to the downstream service
            // Use Mono.defer to ensure fresh invocation on every retry
            return
                    Mono.defer(() -> {
                        //log.info("Sending request to downstream service...");
                        return chain.filter(exchange) // Send the request to the downstream service
                                .onErrorResume(throwable -> {
                                    // Handle upstream (gateway-level) errors
                                    log.error("Error occurred: {}", throwable.getMessage());

                                    if (isUpstreamError(throwable)) {
                                        //log.info("Detected an upstream error, skipping retry.");
                                        // Directly handle upstream errors and bypass downstream retry
                                        return handleErrorFallback(exchange, throwable);
                                    }

                                    // If not upstream, rethrow to allow retry
                                    return Mono.error(throwable);
                                })
                                .then(Mono.defer(() -> handleResponse(responseDecorator, exchange))); // Handle response after downstream call
                    })
                    .retryWhen(retryBackoffSpec) // Retry on errors from the downstream call
                    .onErrorResume(throwable -> handleErrorFallback(exchange, throwable));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isUpstreamError(Throwable throwable) {
        // Identify common upstream (gateway-level) errors that should not trigger retries
        return throwable instanceof org.springframework.cloud.gateway.support.NotFoundException;
    }

    // Ensure the response is captured only after retry completion
    private Mono<Void> handleResponse(BodyCaptureResponse responseDecorator, ServerWebExchange exchange) {
        return responseDecorator.getBody()
                .then(Mono.defer(()->{
                    HttpStatusCode statusCode = responseDecorator.getStatusCode();
                    //log.info("Downstream response status: {}", statusCode);
                    String fullBody = responseDecorator.getFullBody();

                    // Check for 5xx server errors to trigger retry
                    if (statusCode != null && (statusCode.is5xxServerError())) {
                        fullBody += " (5xx error)";//IMPORTANT!!! Keep this here because we are checking this inside the CircuitBreakerFilter
                        log.error("Downstream 5xx error, retrying Body: {}", fullBody);
                        return Mono.error(new RuntimeException(fullBody)); // Trigger retry
                    }

                    // If the response is successful, pass it through without modification
                    if (statusCode != null && statusCode.is2xxSuccessful()) {
                        //log.info("Successful downstream response, returning to client");
                        //exchange.getResponse().setStatusCode(statusCode);
                        
                        // Copy all original headers
                        //exchange.getResponse().getHeaders().putAll(responseDecorator.getHeaders());

                        return exchange.getResponse().setComplete();
                        
                        // Write the original body without any parsing/modification
//                        DataBuffer buffer = exchange.getResponse().bufferFactory()
//                            .wrap(fullBody.getBytes(StandardCharsets.UTF_8));
//                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    }

                    return Mono.error(new RuntimeException("Non-retrievable response status: " + statusCode));
                }));
    }

    public Mono<Void> handleErrorFallback(ServerWebExchange exchange, Throwable throwable) {
        log.error("Error during retry: {} throwable.getClass().getName(): {}", throwable.getMessage(), throwable.getClass().getName());

        // Unwrap the original exception if retries are exhausted
        Throwable unwrappedThrowable = throwable;

        String message = throwable.getMessage();
        // If it's a `RetriesExhaustedException`, extract the original cause
        if (throwable.getClass().getName().equals("reactor.core.Exceptions$RetryExhaustedException")) {
            log.error("Retries exhausted after max attempts. Extracting original exception.");
            unwrappedThrowable = throwable.getCause();  // This should give the original exception
        } else if (throwable.getCause() != null) {
            unwrappedThrowable = throwable.getCause();  // Use the cause if available
        }

        log.error("Original exception message: {} and class name: {}", unwrappedThrowable.getMessage(), unwrappedThrowable.getClass().getName());

        // Propagate the `Authorization` header and other required headers
        HttpHeaders headers = exchange.getRequest().getHeaders();
        if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            log.info("Propagating Authorization header after retries.");
            exchange.getRequest().mutate().header(HttpHeaders.AUTHORIZATION, headers.getFirst(HttpHeaders.AUTHORIZATION));
        }

        HttpStatusCode statusCode = HttpStatus.INTERNAL_SERVER_ERROR; // Default to 500 Internal Server Error

        // Set the appropriate status code based on the type of error
        if (unwrappedThrowable instanceof TimeoutException) {
            return Mono.error(unwrappedThrowable);
        } else if (unwrappedThrowable instanceof org.springframework.cloud.gateway.support.NotFoundException) {
            return Mono.error(unwrappedThrowable);
        } else if(unwrappedThrowable instanceof RuntimeException){
            return Mono.error(unwrappedThrowable);
        } else if (throwable.getClass().getName().equals("reactor.core.Exceptions$RetryExhaustedException")){
            return Mono.error(new RuntimeException(throwable.getMessage() + " => " + unwrappedThrowable.getMessage()  + " " + statusCode));
        }

        // You can add more exception-to-status mappings as needed
        exchange.getResponse().setStatusCode(statusCode);
        byte[] bytes = unwrappedThrowable.getMessage().getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private RetryBackoffSpec buildRetryBackoffSpec(int maxAttempts, Duration waitDuration, RetryConfig retryConfig) {
        return reactor.util.retry.Retry.fixedDelay(maxAttempts, waitDuration)
                .filter(throwable -> {
                    if (throwable instanceof TimeoutException ||
                            (throwable instanceof WebClientResponseException &&
                                    ((WebClientResponseException) throwable).getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE)) {
                        log.info("Skipping retry for TimeoutException or 503 Service Unavailable.");
                        return false; // Don't retry for 503 or Timeout
                    }

                    // Apply the same Resilience4j retryOnException logic to Reactor
                    boolean shouldRetry = retryConfig.getExceptionPredicate().test(throwable);
                    if (!shouldRetry) {
                        log.info("Skipping retry for exception: {} on route: {} seconds: {}", throwable.getClass().getName(), retryRecord.routeId(), waitDuration.getSeconds());
                    }
                    return shouldRetry;
                })
                .doBeforeRetry(retrySignal -> {
                    log.info("Retrying attempt {} for route {} at time {}", retrySignal.totalRetries() + 1, retryRecord.routeId(), Instant.now());
                });
    }

    private boolean shouldRetryOnException(Throwable throwable, String retryExceptions) {
        if (throwable == null) {
            log.warn("Throwable is null in shouldRetryOnException");
            return false;
        }
        
        //log.info("inside shouldRetryOnException");
        String message = throwable.getMessage();
        
        // Check if it's a RuntimeException and contains '503' message
        if (throwable instanceof RuntimeException && 
            message != null && 
            message.contains("503 SERVICE_UNAVAILABLE")) {
            //log.info("Should not retry for the error 503 Service Unavailable.");
            return false; // Skip retry for 503 errors
        }

        if(message != null && message.contains("429 TOO_MANY_REQUESTS")) {
            log.error("Should not retry for the error 429 TOO_MANY_REQUESTS");
            return false;  // Skip retry for 429 TOO_MANY_REQUESTS
        }

        // If it's a WebClientResponseException, check for specific status codes
        if (throwable instanceof WebClientResponseException webClientEx) {
            if (webClientEx.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                log.error("Skipping retry for 503 Service Unavailable.");
                return false; // Skip retry for 503 errors
            }
        }

        // Other retry logic (e.g., for TimeoutException)
        if (throwable instanceof TimeoutException) {
            log.error("Skipping retry for TimeoutException.");
            return false;
        }

        if (retryExceptions != null && !retryExceptions.isEmpty()) {
            for (String exceptionClassName : retryExceptions.split(",")) {
                try {
                    Class<?> clazz = Class.forName(exceptionClassName.trim());
                    if (clazz.isInstance(throwable)) {
                        log.info("Will retry for exception: {}", throwable.getClass().getName());
                        return true;
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Invalid exception class: {}", exceptionClassName);
                }
            }
        }
        log.info("No retry configured for exception: {}", throwable.getClass().getName());
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 4;
    }
}
