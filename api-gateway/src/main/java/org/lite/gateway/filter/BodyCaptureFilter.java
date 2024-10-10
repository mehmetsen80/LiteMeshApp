//package org.lite.gateway.filter;
//
//import lombok.extern.slf4j.Slf4j;
//import org.lite.gateway.service.SharedErrorContext;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.lang.NonNull;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//
//@Component
//@Slf4j
//public class BodyCaptureFilter implements WebFilter {
//
//    @Autowired
//    private SharedErrorContext sharedErrorContext;
//
//    @Override
//    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, WebFilterChain chain) {
////        // Create a BodyCaptureExchange to wrap request/response
////        BodyCaptureExchange bodyCaptureExchange = new BodyCaptureExchange(exchange);
//
//        String requestId = exchange.getRequest().getId(); // Unique ID for the request
//        BodyCaptureRequest requestDecorator = new BodyCaptureRequest(exchange.getRequest());
//        BodyCaptureResponse responseDecorator = new BodyCaptureResponse(exchange.getResponse());
//
//        ServerWebExchange mutatedExchange = exchange.mutate()
//                .request(requestDecorator)
//                .response(responseDecorator)
//                .build();
//
//        // Use the custom decorated exchange to process the filter chain
//        return chain.filter(mutatedExchange)
//                .doOnError(throwable -> {
//                    // If an error occurs, capture it and store it in exchange attributes
//                    exchange.getAttributes().put("originalError", throwable);
//                    log.error("Error captured in BodyCaptureFilter: {}", throwable.getMessage());
//                    // Store the error in Redis
//                    sharedErrorContext.storeError(requestId, throwable);
//                })
//                .doOnSuccess(aVoid -> {
////                    // After successful exchange, you can log request and response bodies here
////                    String capturedRequestBody = bodyCaptureExchange.getRequest().getFullBody();
////                    String capturedResponseBody = bodyCaptureExchange.getResponse().getFullBody();
////                    exchange.getAttributes().put("originalError", capturedResponseBody);
////
////                    log.info("Captured request body: {}", capturedRequestBody);
////                    log.info("Captured response body: {}", capturedResponseBody);
//
//
//                    // Optionally capture the request and response bodies for logging
//                    String requestBody = requestDecorator.getFullBody();
//                    String responseBody = responseDecorator.getFullBody();
//                    log.info("Captured Request Body: {}", requestBody);
//                    log.info("Captured Response Body: {}", responseBody);
//                    exchange.getAttributes().put("originalError", responseBody);
//
//
//                    // Store the error in Redis
//                    log.info("1st requestId: {}", requestId);
//                    sharedErrorContext.storeError(requestId, new RuntimeException(responseBody));
//                    log.error("Stored error captured in Redis for request: {}", requestId);
//                });
//    }
//}
