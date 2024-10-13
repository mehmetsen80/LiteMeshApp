package org.lite.gateway.filter.retry;

import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.BodyCaptureRequest;
import org.lite.gateway.filter.BodyCaptureResponse;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;


@Slf4j
public class RetryFilterStrategyOld3 implements  GatewayFilter, Ordered {

    //private String routeId;
    private final RetryBackoffSpec retryBackoffSpec;
    private final ApiRoute apiRoute;
    private final String fallbackUri;
    private boolean retryCompleted = false;  // Flag to track retry completion


    public RetryFilterStrategyOld3(ApiRoute apiRoute, FilterConfig filter){
        int maxAttempts = Integer.parseInt(filter.getArgs().get("maxAttempts"));
        Duration waitDuration = Duration.parse(filter.getArgs().get("waitDuration"));
        String retryExceptions = filter.getArgs().get("retryExceptions"); // Optional
        fallbackUri = filter.getArgs().get("fallbackUri");
        this.apiRoute = apiRoute;

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(waitDuration)
                .retryOnException(throwable -> shouldRetryOnException(throwable, retryExceptions))
                .build();

        retryBackoffSpec = buildRetryBackoffSpec(maxAttempts, waitDuration, retryConfig);
    }

    private RetryBackoffSpec buildRetryBackoffSpec(int maxAttempts, Duration waitDuration, RetryConfig retryConfig) {
        return reactor.util.retry.Retry.fixedDelay(maxAttempts, waitDuration)
                .filter(throwable -> {
                    log.info("throwable message: {}", throwable.getMessage());
                    //log.info(String.valueOf(throwable));
                    // Apply the same Resilience4j retryOnException logic to Reactor
                    boolean shouldRetry = retryConfig.getExceptionPredicate().test(throwable);
                    if (!shouldRetry) {
                        log.info("Skipping retry for exception: {} on route: {} seconds: {}", throwable.getClass().getName(), apiRoute.getRouteIdentifier(), waitDuration.getSeconds());
                    }
                    return shouldRetry;
                })
                .doBeforeRetry(retrySignal -> {
                    log.info("Retrying attempt {} for route {} at time {}", retrySignal.totalRetries() + 1, apiRoute.getRouteIdentifier(), Instant.now());
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

//    public Mono<Void> handleResponse(BodyCaptureResponse responseDecorator, ServerWebExchange exchange) {
//        HttpStatusCode statusCode = responseDecorator.getStatusCode();
//        log.info("Downstreamm response status: {}", statusCode);
//
//        if (statusCode != null && statusCode.is5xxServerError()) {
//            String errorMessage = responseDecorator.getFullBody();
//            DataBuffer buffer = exchange.getResponse().bufferFactory()
//                    .wrap(errorMessage.getBytes(StandardCharsets.UTF_8));
//            log.info("bufferr: {} ", buffer);
//            exchange.getResponse().writeWith(Mono.just(buffer));
//
//            // Write the response back and return an error to trigger retry
////            return exchange.getResponse().writeWith(Mono.just(buffer))
////                    .then(Mono.error(new RuntimeException("Server error: " + errorMessage)));
//
//            return Mono.error(new RuntimeException("Server errorr: " + errorMessage));
//        }
//        return Mono.empty();
//    }

    private Mono<Void> handleResponse(BodyCaptureResponse responseDecorator, ServerWebExchange exchange) {
        return responseDecorator.getBody()
                .doOnNext(fullBody -> {
                    log.info("Fullbody captured: {}", fullBody);
//                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
//                    exchange.getResponse().getHeaders().setContentLength(fullBody.getBytes(StandardCharsets.UTF_8).length);
                })
                .flatMap(fullBody -> {

                    // Skip re-processing if retry has already completed
                    if (retryCompleted) {
                        log.info("Retry already completed, skipping re-capture.");
                        return Mono.empty();
                    }

                    HttpStatusCode statusCode = responseDecorator.getStatusCode();
                    log.info("Downstream response status: {}", statusCode);

                    // Check for 5xx server errors to trigger retry
                    if (statusCode != null && (statusCode.is5xxServerError() || statusCode.is3xxRedirection())) {
                        String errorMessage = responseDecorator.getFullBody();
                        log.error("Downstream error, retrying: {}", errorMessage);
                        return Mono.error(new RuntimeException(errorMessage)); // Trigger retry
                    }

//                    // Successful response, proceed normally
//                    return Mono.just(responseDecorator).flatMap(resp -> {
//                        DataBuffer buffer = exchange.getResponse().bufferFactory()
//                                .wrap(resp.getFullBody().getBytes(StandardCharsets.UTF_8));
//                        return exchange.getResponse().writeWith(Mono.just(buffer));  // Send the final response
//                    });

                    return Mono.empty();

//                    // Success: Write the successful response body after retries
//                    log.info("Successful downstream response, returning to client");
//                    if (fullBody != null && !fullBody.isEmpty()) {
//                        log.info("Fullbody: {}", fullBody );
//                        // Set necessary response headers
//                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);  // or appropriate content type
//                        exchange.getResponse().getHeaders().setContentLength(fullBody.getBytes(StandardCharsets.UTF_8).length); // Set content length
//
//                        // Write the full response body to client
//                        DataBuffer buffer = exchange.getResponse().bufferFactory()
//                                .wrap(fullBody.getBytes(StandardCharsets.UTF_8));
//                        log.info("Before writing response: status={}, headers={}", exchange.getResponse().getStatusCode(), exchange.getResponse().getHeaders());
//                        return exchange.getResponse().writeWith(Mono.just(buffer))
//                                .then(exchange.getResponse().setComplete()) // Ensure response completion
//                                .doOnTerminate(() -> {
//                                    log.info("Response body flushed and completed.");
//                                });
//                    } else {
//                        log.warn("Response body is null, no data to write to client.");
//                    }
//
//                    // Complete if no body to write
//                    return exchange.getResponse().setComplete();

                    //return Mono.empty();
                })
                .then()
                .onErrorResume(throwable -> { // Handle network errors or any other throwable, catches the gateway (upstream exceptions)
                    // Handle upstream (pre-response) errors
                    log.error("Downstream error encountered: {}", throwable.getMessage());

                    //exchange.getResponse().setComplete();
                    return Mono.error(throwable);
                })
//                .then(exchange.getResponse().setComplete())
                .switchIfEmpty(Mono.defer(() -> {
                    // Handling case where body is missing or null
                    log.warn("No response body captured, completing the exchange without body");
                    //exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);  // or appropriate content type
                    return exchange.getResponse().setComplete();
                }));
    }





//    private Mono<Void> handleResponse(BodyCaptureResponse responseDecorator, ServerWebExchange exchange) {
//        String fullBody = responseDecorator.getFullBody(); // Get the captured response body
//        HttpStatusCode statusCode = responseDecorator.getStatusCode();
//        log.info("Downstream response status: {}", statusCode);
//
//        if (statusCode != null && statusCode.is5xxServerError()) {
//            log.error("Downstream 5xx error, retrying: {}", fullBody);
//            return Mono.error(new RuntimeException(fullBody)); // Trigger retry
//        }
//
//        // Success: Write the successful response body after retries
//        log.info("Successful downstream response, returning to client");
//        if (fullBody != null) {
//            log.info("Fullbody: {}", fullBody);
//
//            // Set necessary response headers
//            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);  // or appropriate content type
//            exchange.getResponse().getHeaders().setContentLength(fullBody.getBytes(StandardCharsets.UTF_8).length); // Set content length
//
//            // Write the full response body to client
//            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(fullBody.getBytes(StandardCharsets.UTF_8));
//            return exchange.getResponse().writeWith(Mono.just(buffer))
//                    .doOnTerminate(() -> log.info("Response body flushed to the client"));
//        } else {
//            log.warn("Response body is null, no data to write to client.");
//        }
//
//        // Complete if no body to write
//        return exchange.getResponse().setComplete();
//    }


    public Mono<Void> handleErrorFallback(ServerWebExchange exchange, Throwable throwable) {
        log.error("Error during retry: {}", throwable.getMessage());
        // Propagate the `Authorization` header and other required headers
        HttpHeaders headers = exchange.getRequest().getHeaders();
        if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            log.info("Propagating Authorization header after retries.");
            exchange.getRequest().mutate().header(HttpHeaders.AUTHORIZATION, headers.getFirst(HttpHeaders.AUTHORIZATION));
        }


//        // If a fallback URL is configured, redirect to fallback or handle accordingly
//        if (fallbackUri != null) {
//            exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
//            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
//            log.info("fallbackUri: {}", fallbackUri);
//            String fallbackUrlWithException = fallbackUri + "?exceptionMessage=" + URLEncoder.encode(throwable.getMessage(), StandardCharsets.UTF_8);
//            exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUrlWithException));
//            return exchange.getResponse().setComplete();
//        }

        // If a fallback URL is configured, programmatically forward the request to the fallback URI
        // If a fallback URL is configured, programmatically forward the request to the fallback URI
        if (fallbackUri != null) {
            log.info("Handling fallback internally for URI: {}", fallbackUri);

            // Mutate the request to the fallback URI
            String fallbackUrlWithException = fallbackUri + "?exceptionMessage=" + URLEncoder.encode(throwable.getMessage(), StandardCharsets.UTF_8);
            URI fallbackUrl = URI.create(fallbackUrlWithException);
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate().uri(fallbackUrl).build())  // Update the request URI to the fallback URI
                    .build();
            return mutatedExchange.getResponse().setComplete();

            // Forward the request to the fallback route by re-invoking the chain with the mutated exchange
            //return chain.filter(mutatedExchange);
        }


        // Otherwise, throw the exception to be handled by GlobalExceptionHandler
        return Mono.error(throwable);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        BodyCaptureRequest requestDecorator = new BodyCaptureRequest(exchange.getRequest());
        BodyCaptureResponse responseDecorator = new BodyCaptureResponse(exchange.getResponse());
        ServerWebExchange mutatedExchange = exchange.mutate()
                //.request(requestDecorator)
                .response(responseDecorator)
                .build();

        return chain.filter(mutatedExchange)
                .onErrorResume(throwable -> { // Handle network errors or any other throwable, catches the gateway (upstream exceptions)
                    // Handle upstream (pre-response) errors
                    log.error("Upstream error encountered: {}", throwable.getMessage());
                    return Mono.error(throwable);
                })
                .then(Mono.defer(() -> handleResponse(responseDecorator, mutatedExchange)))
//                .then(Mono.defer(() -> {
//                    //log.info("resp: {}", resp);
//                    log.info("then");
//                            // Successful response, proceed normally
//                            return responseDecorator.getBody().flatMap(fullBody -> {
//
////                        String fullBody = Mono.from(resp).toString();
//                                HttpStatusCode statusCode = responseDecorator.getStatusCode();
//                                log.info("Downstream response status: {}", statusCode);
//
//                                // Check for 5xx server errors to trigger retry
//                                if (statusCode != null && statusCode.is5xxServerError()) {
//                                    String errorMessage = responseDecorator.getFullBody();
//                                    log.error("Downstream 5xx error, retrying: {}", errorMessage);
//                                    return Mono.error(new RuntimeException(errorMessage)); // Trigger retry
//                                }
//
//                                log.info("resp.getFullBody() {}", fullBody);
//                                mutatedExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);  // or appropriate content type
//                                mutatedExchange.getResponse().getHeaders().setContentLength(fullBody.getBytes(StandardCharsets.UTF_8).length); // Set content length
//                                DataBuffer buffer = mutatedExchange.getResponse().bufferFactory()
//                                        .wrap(fullBody.getBytes(StandardCharsets.UTF_8));
//                                //log.info("lets see the final buffer: {}", buffer);
//                                return mutatedExchange.getResponse().writeWith(Mono.just(buffer));// Send the final response
//
//                                //return Mono.empty();
//                            });
//                        }
//
//
//
//
////                    // Success: Write the successful response body after retries
////                    log.info("Successful downstream response, returning to client");
////                    if (!fullBody.isEmpty()) {
////                        log.info("Fullbody: {}", fullBody );
////                        // Set necessary response headers
////                        mutatedExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);  // or appropriate content type
////                        mutatedExchange.getResponse().getHeaders().setContentLength(fullBody.getBytes(StandardCharsets.UTF_8).length); // Set content length
//
//////                        // Write the full response body to client
//////                        DataBuffer buffer = mutatedExchange.getResponse().bufferFactory()
//////                                .wrap(fullBody.getBytes(StandardCharsets.UTF_8));
//////                        log.info("Before writing response: status={}, headers={}", mutatedExchange.getResponse().getStatusCode(), mutatedExchange.getResponse().getHeaders());
//////                         return mutatedExchange.getResponse().writeWith(Mono.just(buffer))
//////                                .then(mutatedExchange.getResponse().setComplete()) // Ensure response completion
//////                                .doOnTerminate(() -> {
//////                                    log.info("Response body flushed and completed.");
//////                                });
////
////
////
////                    } else {
////                        log.warn("Response body is null, no data to write to client.");
////                    }
////
////                    // Complete if no body to write
////                    return mutatedExchange.getResponse().setComplete();
//
//                )
//                        .then(mutatedExchange.getResponse().setComplete())
//                        )
//                .then(Mono.defer(() -> {
//                            String fullBody = responseDecorator.getFullBody();
//                            HttpStatusCode statusCode = responseDecorator.getStatusCode();
//                            log.info("Downstream response status: {}", statusCode);
//
//                            // Check for 5xx server errors to trigger retry
//                            if (statusCode != null && statusCode.is5xxServerError()) {
//                                String errorMessage = responseDecorator.getFullBody();
//                                log.error("Downstream 5xx error, retrying: {}", errorMessage);
//                                return Mono.error(new RuntimeException(errorMessage)); // Trigger retry
//                            }
//
////                    // Successful response, proceed normally
////                    return Mono.just(responseDecorator).flatMap(resp -> {
////                        DataBuffer buffer = exchange.getResponse().bufferFactory()
////                                .wrap(resp.getFullBody().getBytes(StandardCharsets.UTF_8));
////                        return exchange.getResponse().writeWith(Mono.just(buffer));  // Send the final response
////                    });
//
//                    //return Mono.empty();
//
//                            // Success: Write the successful response body after retries
//                            log.info("Successful downstream response, returning to client");
//                            if (fullBody != null && !fullBody.isEmpty()) {
//                                log.info("Fullbody: {}", fullBody );
//                                // Set necessary response headers
//                                mutatedExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);  // or appropriate content type
//                                mutatedExchange.getResponse().getHeaders().setContentLength(fullBody.getBytes(StandardCharsets.UTF_8).length); // Set content length
//
//                                // Write the full response body to client
//                                DataBuffer buffer = mutatedExchange.getResponse().bufferFactory()
//                                        .wrap(fullBody.getBytes(StandardCharsets.UTF_8));
//                                log.info("Before writing response: status={}, headers={}", mutatedExchange.getResponse().getStatusCode(), mutatedExchange.getResponse().getHeaders());
//                                return mutatedExchange.getResponse().writeWith(Mono.just(buffer))
//                                        .then(mutatedExchange.getResponse().setComplete()) // Ensure response completion
//                                        .doOnTerminate(() -> {
//                                            log.info("Response body flushed and completed.");
//                                        });
//                            } else {
//                                log.warn("Response body is null, no data to write to client.");
//                            }
//
//                            // Complete if no body to write
//                            return mutatedExchange.getResponse().setComplete();
//                        })
                .retryWhen(retryBackoffSpec)  // Retry on errors
                //.doOnSuccess(aVoid -> retryCompleted = true)  // Mark retry as completed
                .onErrorResume(throwable -> {
                    log.error("Retries failed for route: {}", apiRoute.getRouteIdentifier());
                    return handleErrorFallback(mutatedExchange, throwable);
                });

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
