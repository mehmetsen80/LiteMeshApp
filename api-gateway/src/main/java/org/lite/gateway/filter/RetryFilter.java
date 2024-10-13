package org.lite.gateway.filter;

import io.github.resilience4j.retry.RetryConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.filter.BodyCaptureRequest;
import org.lite.gateway.filter.BodyCaptureResponse;
import org.lite.gateway.model.RetryRecord;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Data
public class RetryFilter implements GatewayFilter, Ordered {

    RetryRecord retryRecord;

    public RetryFilter(RetryRecord retryRecord) {
        this.retryRecord = retryRecord;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String routeId = retryRecord.routeId();
        int maxAttempts = retryRecord.maxAttempts();
        Duration waitDuration = retryRecord.waitDuration();
        String retryExceptions = retryRecord.retryExceptions();
        log.info("Applying Retry for route: {}", routeId);

        try {
            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(maxAttempts)
                    .waitDuration(waitDuration)
                    .retryOnException(throwable -> shouldRetryOnException(throwable, retryExceptions))
                    .build();
            RetryBackoffSpec retryBackoffSpec = buildRetryBackoffSpec(maxAttempts, waitDuration, retryConfig);
            BodyCaptureRequest requestDecorator = new BodyCaptureRequest(exchange.getRequest());
            BodyCaptureResponse responseDecorator = new BodyCaptureResponse(exchange.getResponse());
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(requestDecorator)
                    .response(responseDecorator)
                    .build();

//            return chain.filter(mutatedExchange)
//                    .onErrorResume(throwable -> { // Handle network errors or any other throwable, catches the gateway (upstream exceptions)
//                        // Handle upstream (pre-response) errors
//                        log.error("Upstream error encountered: {}", throwable.getMessage());
//                        return Mono.error(throwable);
//                    })
//                    .then(handleResponse(responseDecorator, mutatedExchange))  // Handle the response after downstream
//                    //.flatMap(response -> handleResponse(responseDecorator, mutatedExchange)) // Handle response downstream
//                    .retryWhen(retryBackoffSpec) // Retry on downstream errors
//                    .onErrorResume(throwable -> handleErrorFallback(mutatedExchange, throwable));



            // First, handle upstream errors BEFORE retry logic
            return chain.filter(mutatedExchange)
                    .onErrorResume(throwable -> {
                        log.info("inside onErrorResume()");
                        if (isUpstreamError(throwable)) {
                            log.info("Upstream error detected: {}, bypassing retry.", throwable.getMessage());
                            return handleErrorFallback(mutatedExchange, throwable);  // Handle upstream error, no retry
                        }
                        // If it's not an upstream error, propagate the error for retry handling
                        return Mono.error(throwable);
                    })
                    .then(Mono.defer(() -> handleResponse(responseDecorator, mutatedExchange))) // Handle response from downstream
                    .retryWhen(retryBackoffSpec)  // Retry on downstream errors
                    .onErrorResume(throwable -> handleErrorFallback(mutatedExchange, throwable));  // Handle retries if exhausted



            // We wrap the entire downstream call (chain.filter) inside the retry logic
            // We use Mono.defer to make sure that each retry triggers a fresh request to the downstream service
            // Use Mono.defer to ensure fresh invocation on every retry
//            return
//
//                    Mono.defer(() -> {
//                        log.info("Sending request to downstream service...");
//                        return chain.filter(mutatedExchange) // Send the request to the downstream service
//                                .onErrorResume(throwable -> {
//                                    // Handle upstream (gateway-level) errors
//                                    log.error("Error occurred: {}", throwable.getMessage());
//
//                                    if (isUpstreamError(throwable)) {
//                                        log.info("Detected an upstream error, skipping retry.");
//                                        // Directly handle upstream errors and bypass downstream retry
//                                        return handleErrorFallback(mutatedExchange, throwable);
//                                    }
//
//                                    // If not upstream, rethrow to allow retry
//                                    return Mono.error(throwable);
//                                })
//                                .then(Mono.defer(() -> handleResponse(responseDecorator, mutatedExchange))) // Handle response after downstream call
//                                .onErrorResume(throwable -> {
//                                    log.error("Downstream error encountered: {}", throwable.getMessage());
//                                    return Mono.error(throwable);
//                                });
//                    })
//                    .retryWhen(retryBackoffSpec) // Retry on errors from the downstream call
//                    .onErrorResume(throwable -> handleErrorFallback(mutatedExchange, throwable)); // Handle errors with fallback

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
                .doOnNext(fullBody -> {
                    log.info("Fullbody captured: {}", fullBody);
                })
                .flatMap(fullBody -> {
                    HttpStatusCode statusCode = responseDecorator.getStatusCode();
                    log.info("Downstream response status: {}", statusCode);

                    // Check for 5xx server errors to trigger retry
                    if (statusCode != null && statusCode.is5xxServerError()) {
                        //String errorMessage = responseDecorator.getFullBody();
                        log.error("Downstream 5xx error, retrying: {}", fullBody);
                        return Mono.error(new RuntimeException(fullBody)); // Trigger retry
                    }

                    // If the response is successful, complete the response correctly
                    if (statusCode != null && statusCode.is2xxSuccessful()) {
                        log.info("Successful downstream response, returning to client");
                        exchange.getResponse().setStatusCode(statusCode); // Set the response status code
                        exchange.getResponse().getHeaders().addAll(responseDecorator.getHeaders()); // Copy headers if necessary
                        DataBuffer buffer = exchange.getResponse().bufferFactory()
                                       .wrap(fullBody.getBytes(StandardCharsets.UTF_8));
                        return exchange.getResponse().writeWith(Mono.just(buffer)); // Write the response body
                    }

                    // Handle other unexpected status codes
//                    log.warn("Unexpected status code: {}", statusCode);
//                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
//                    return exchange.getResponse().setComplete(); // Completes without body

                    // Handle other status codes (unexpected ones)
                    log.warn("Unexpected status code: {}", statusCode);
                    return Mono.empty(); // Or handle other cases as needed

                }).then();
//                .switchIfEmpty(Mono.defer(() -> {
//                    // Handling case where body is missing or null
//                    log.warn("No response body captured, completing the exchange without body");
//                    return exchange.getResponse().setComplete();
//                }));
    }

    public Mono<Void> handleErrorFallback(ServerWebExchange exchange, Throwable throwable) {
        log.error("Error during retry: {}", throwable.getMessage());
        // Propagate the `Authorization` header and other required headers
        HttpHeaders headers = exchange.getRequest().getHeaders();
        if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            log.info("Propagating Authorization header after retries.");
            exchange.getRequest().mutate().header(HttpHeaders.AUTHORIZATION, headers.getFirst(HttpHeaders.AUTHORIZATION));
        }

        String fallbackUri = retryRecord.fallbackUrl();
        // If a fallback URL is configured, redirect to fallback or handle accordingly
        if (fallbackUri != null) {
            log.info("AARedirecting to fallback URI: {}", fallbackUri);

            // Mutate the response to redirect to the fallback URL
            exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
            String fallbackUrlWithException = fallbackUri + "?exceptionMessage=" + URLEncoder.encode(throwable.getMessage(), StandardCharsets.UTF_8);
            exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUrlWithException));
            return exchange.getResponse().setComplete();
        }

//        return Mono.error(throwable);  // Re-throw or handle fallback as required

        log.warn("No fallback URL configured. Returning SERVICE_UNAVAILABLE.");
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().setComplete();
    }

    private RetryBackoffSpec buildRetryBackoffSpec(int maxAttempts, Duration waitDuration, RetryConfig retryConfig) {
        return reactor.util.retry.Retry.fixedDelay(maxAttempts, waitDuration)
                .filter(throwable -> {
                    log.info("throwable message: {}", throwable.getMessage());
                    //log.info(String.valueOf(throwable));
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
        if (retryExceptions != null && !retryExceptions.isEmpty()) {
            for (String exceptionClassName : retryExceptions.split(",")) {
                try {
                    Class<?> clazz = Class.forName(exceptionClassName.trim());
                    if (clazz.isInstance(throwable)) {
                        return true;
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Invalid exception class: {}", exceptionClassName);
                }
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 4;
    }
}
