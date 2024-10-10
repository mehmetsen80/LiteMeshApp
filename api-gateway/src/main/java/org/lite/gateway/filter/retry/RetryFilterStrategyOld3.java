//package org.lite.gateway.filter.retry;
//
//import io.github.resilience4j.retry.RetryConfig;
//import lombok.extern.slf4j.Slf4j;
//import org.lite.gateway.entity.ApiRoute;
//import org.lite.gateway.entity.FilterConfig;
//import org.lite.gateway.filter.BodyCaptureRequest;
//import org.lite.gateway.filter.BodyCaptureResponse;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.core.Ordered;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.HttpStatusCode;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//import reactor.util.retry.RetryBackoffSpec;
//
//import java.nio.charset.StandardCharsets;
//import java.time.Duration;
//import java.time.Instant;
//
//
//@Slf4j
//public class RetryFilterStrategyOld3 implements  GatewayFilter, Ordered {
//
//    //private String routeId;
//    private final RetryBackoffSpec retryBackoffSpec;
//    private final ApiRoute apiRoute;
//
//
//    public RetryFilterStrategyOld3(ApiRoute apiRoute, FilterConfig filter){
//        int maxAttempts = Integer.parseInt(filter.getArgs().get("maxAttempts"));
//        Duration waitDuration = Duration.parse(filter.getArgs().get("waitDuration"));
//        String retryExceptions = filter.getArgs().get("retryExceptions"); // Optional
//        //String fallbackUri = filter.getArgs().get("fallbackUri");
//        this.apiRoute = apiRoute;
//
//        RetryConfig retryConfig = RetryConfig.custom()
//                .maxAttempts(maxAttempts)
//                .waitDuration(waitDuration)
//                .retryOnException(throwable -> shouldRetryOnException(throwable, retryExceptions))
//                .build();
//
//        retryBackoffSpec = buildRetryBackoffSpec(maxAttempts, waitDuration, retryConfig);
//    }
//
//    private RetryBackoffSpec buildRetryBackoffSpec(int maxAttempts, Duration waitDuration, RetryConfig retryConfig) {
//        return reactor.util.retry.Retry.fixedDelay(maxAttempts, waitDuration)
//                .filter(throwable -> {
//                    log.info("throwable message: {}", throwable.getMessage());
//                    log.info(String.valueOf(throwable));
//                    // Apply the same Resilience4j retryOnException logic to Reactor
//                    boolean shouldRetry = retryConfig.getExceptionPredicate().test(throwable);
//                    if (!shouldRetry) {
//                        log.info("Skipping retry for exception: {} on route: {} seconds: {}", throwable.getClass().getName(), apiRoute.getRouteIdentifier(), waitDuration.getSeconds());
//                    }
//                    return shouldRetry;
//                })
//                .doBeforeRetry(retrySignal -> {
//                    log.info("Retrying attempt {} for route {} at time {}", retrySignal.totalRetries() + 1, apiRoute.getRouteIdentifier(), Instant.now());
//                });
//    }
//
//    private boolean shouldRetryOnException(Throwable throwable, String retryExceptions) {
//        if (retryExceptions != null && !retryExceptions.isEmpty()) {
//            for (String exceptionClassName : retryExceptions.split(",")) {
//                try {
//                    Class<?> clazz = Class.forName(exceptionClassName.trim());
//                    if (clazz.isInstance(throwable)) {
//                        return true;
//                    }
//                } catch (ClassNotFoundException e) {
//                    log.warn("Invalid exception class: {}", exceptionClassName);
//                }
//            }
//        }
//        return false;
//    }
//
//    public Mono<Void> handleResponse(BodyCaptureResponse responseDecorator, ServerWebExchange exchange) {
//        HttpStatusCode statusCode = responseDecorator.getStatusCode();
//        log.info("Downstream response status: {}", statusCode);
//
//        if (statusCode != null && statusCode.is5xxServerError()) {
//            String errorMessage = responseDecorator.getFullBody();
//            DataBuffer buffer = exchange.getResponse().bufferFactory()
//                    .wrap(errorMessage.getBytes(StandardCharsets.UTF_8));
//            log.info("buffer: {} ", buffer);
//            exchange.getResponse().writeWith(Mono.just(buffer));
//
//            // Write the response back and return an error to trigger retry
////            return exchange.getResponse().writeWith(Mono.just(buffer))
////                    .then(Mono.error(new RuntimeException("Server error: " + errorMessage)));
//
//            return Mono.error(new RuntimeException("Server error: " + errorMessage));
//        }
//        return Mono.empty();
//    }
//
//    public Mono<Void> handleErrorFallback(ServerWebExchange exchange, Throwable throwable) {
//        log.error("Error during retry: {}", throwable.getMessage());
//        // Handle fallback logic here (e.g., redirect to fallback URI)
//        exchange.getResponse().setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
//        return exchange.getResponse().setComplete();
//        //return Mono.error(throwable);  // Re-throw or handle fallback as required
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        BodyCaptureRequest requestDecorator = new BodyCaptureRequest(exchange.getRequest());
//        BodyCaptureResponse responseDecorator = new BodyCaptureResponse(exchange.getResponse());
//        ServerWebExchange mutatedExchange = exchange.mutate()
//                .request(requestDecorator)
//                .response(responseDecorator)
//                .build();
//
//        return chain.filter(mutatedExchange)
//                .onErrorResume(throwable -> { // Handle network errors or any other throwable, catches the gateway (upstream exceptions)
//                    // Handle upstream (pre-response) errors
//                    log.error("Upstream error encountered: {}", throwable.getMessage());
//                    return Mono.error(throwable);
//                })
//                .then(Mono.defer(() -> handleResponse(responseDecorator, mutatedExchange)))
//                .retryWhen(retryBackoffSpec)  // Retry on errors
//                .onErrorResume(throwable -> {
//                    log.error("Retries failed for route: {}", apiRoute.getRouteIdentifier());
//                    return handleErrorFallback(mutatedExchange, throwable);
//                });
//
//    }
//
//    @Override
//    public int getOrder() {
//        return Ordered.HIGHEST_PRECEDENCE + 2;
//    }
//}
