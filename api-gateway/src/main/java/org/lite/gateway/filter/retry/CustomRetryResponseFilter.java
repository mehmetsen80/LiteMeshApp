package org.lite.gateway.filter.retry;

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
public class CustomRetryResponseFilter implements GatewayFilter, Ordered  {

    RetryRecord retryRecord;

    public CustomRetryResponseFilter(RetryRecord retryRecord){
        this.retryRecord = retryRecord;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String routeId = retryRecord.routeId();
        int maxAttempts = retryRecord.maxAttempts();
        Duration waitDuration = retryRecord.waitDuration();
        String retryExceptions = retryRecord.retryExceptions();
        String fallbackUri = retryRecord.fallbackUrl();
        log.info("Applying Retry for route: {}", routeId);
        try{
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

            return chain.filter(mutatedExchange)
                    .onErrorResume(throwable -> { // Handle network errors or any other throwable, catches the gateway (upstream exceptions)
                        // Handle upstream (pre-response) errors
                        log.error("Upstream error encountered: {}", throwable.getMessage());
                        return Mono.error(throwable);
                    })
                    .then(Mono.defer(() -> handleResponse(responseDecorator, mutatedExchange)))
                    .retryWhen(retryBackoffSpec)  // Retry on errors
                    //.then(Mono.defer(() -> finalResponseHandling(responseDecorator, mutatedExchange)))
                    .onErrorResume(throwable -> handleErrorFallback(mutatedExchange, fallbackUri, throwable));

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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

//    private Mono<Void> handleResponse(BodyCaptureResponse responseDecorator, ServerWebExchange exchange) {
//        HttpStatusCode statusCode = responseDecorator.getStatusCode();
//        log.info("Downstream response status: {}", statusCode);
//
//        if (statusCode != null && statusCode.is5xxServerError()) {
//            String errorMessage = responseDecorator.getFullBody();
//            DataBuffer buffer = exchange.getResponse().bufferFactory()
//                    .wrap(errorMessage.getBytes(StandardCharsets.UTF_8));
//            exchange.getResponse().writeWith(Mono.just(buffer));
//            return Mono.error(new RuntimeException(errorMessage));
//        }
//
//        log.info("Success response, no retry required");
//        return Mono.empty();
//    }

    private Mono<Void> handleResponse(BodyCaptureResponse responseDecorator, ServerWebExchange exchange) {
        return responseDecorator.getBody()
                .flatMap(fullBody -> {
                    HttpStatusCode statusCode = responseDecorator.getStatusCode();
                    log.info("Downstream response status: {}", statusCode);

                    // Check for 5xx server errors to trigger retry
                    if (statusCode != null && statusCode.is5xxServerError()) {
                        String errorMessage = responseDecorator.getFullBody();
                        log.error("Downstream 5xx error, retrying: {}", errorMessage);
                        return Mono.error(new RuntimeException(errorMessage)); // Trigger retry
                    }

                    // Successful response, proceed normally
                    return Mono.just(responseDecorator).flatMap(resp -> {
                        DataBuffer buffer = exchange.getResponse().bufferFactory()
                                .wrap(resp.getFullBody().getBytes(StandardCharsets.UTF_8));
                        return exchange.getResponse().writeWith(Mono.just(buffer));  // Send the final response
                    });
                });
    }

//    private Mono<Void> finalResponseHandling(BodyCaptureResponse responseDecorator, ServerWebExchange exchange) {
//        // Final processing of the response after retries are exhausted or completed
//        String fullBody = responseDecorator.getFullBody();
//        if (fullBody != null) {
//            DataBuffer buffer = exchange.getResponse().bufferFactory()
//                    .wrap(fullBody.getBytes(StandardCharsets.UTF_8));
//            return exchange.getResponse().writeWith(Mono.just(buffer));  // Write the final response body
//        }
//
//        return exchange.getResponse().setComplete();  // Complete the response if no body is available
//    }

    private Mono<Void> handleErrorFallback(ServerWebExchange exchange, String fallbackUri, Throwable throwable) {
        log.error("Retries failed for route: {}", retryRecord.routeId());
        log.error("Error during retry: {}", throwable.getMessage());
        // Handle fallback logic here (e.g., redirect to fallback URI)
        exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
        String fallbackUrlWithException = fallbackUri + "?exceptionMessage=" + URLEncoder.encode(throwable.getMessage(), StandardCharsets.UTF_8);
        exchange.getResponse().getHeaders().setLocation(URI.create(fallbackUrlWithException));
        return exchange.getResponse().setComplete();
        //return Mono.error(throwable);  // Re-throw or handle fallback as required
    }

    private RetryBackoffSpec buildRetryBackoffSpec(int maxAttempts, Duration waitDuration, RetryConfig retryConfig) {
        return reactor.util.retry.Retry.fixedDelay(maxAttempts, waitDuration)
                .filter(throwable -> {
                    log.info("throwable message: {}", throwable.getMessage());
                    log.info(String.valueOf(throwable));
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

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
