//package org.lite.gateway.filter.retry;
//
//import lombok.extern.slf4j.Slf4j;
//import org.lite.gateway.filter.BodyCaptureRequest;
//import org.lite.gateway.filter.BodyCaptureResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Component
//@Slf4j
//@Order(Ordered.HIGHEST_PRECEDENCE + 2)
//public class CustomRetryResponseFilter implements GatewayFilter {
//
//    private final RetryFilterStrategy retryFilterStrategy;
//
//    @Autowired
//    public CustomRetryResponseFilter(RetryFilterStrategy retryFilterStrategy) {
//        this.retryFilterStrategy = retryFilterStrategy;
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//
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
//                .then(Mono.defer(() -> retryFilterStrategy.handleResponse(responseDecorator, exchange)))
//                .retryWhen(retryFilterStrategy.getRetryBackoffSpec())  // Retry on errors  // Retry on errors
//                .onErrorResume(throwable -> {
//                    log.error("Retries failed for route: {}", retryFilterStrategy.getRouteId());
//                    return retryFilterStrategy.handleErrorFallback(exchange, throwable);
//                });
//    }
//}
