//package org.lite.gateway.filter;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
//import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
//import org.springframework.core.Ordered;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferUtils;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.net.URI;
//import java.nio.charset.StandardCharsets;
//
//@Slf4j
//public class CustomReactiveLoadBalancerClientFilter extends ReactiveLoadBalancerClientFilter {
//
//    private static final String GATEWAY_HOST = "api-gateway-service";
//
//    public CustomReactiveLoadBalancerClientFilter(LoadBalancerClientFactory clientFactory, GatewayLoadBalancerProperties properties) {
//        super(clientFactory, properties);
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // Log initial request URI to verify it has the expected service name URI (e.g., lb://inventory-service)
//        log.info("Request URI before load balancing: {}", exchange.getRequest().getURI());
//
//        // Forwarding Authorization header if present
//        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
//                .uri(updateUriForLoadBalancing(exchange))
//                .header("X-Forwarded-Host", GATEWAY_HOST)
//                .header(HttpHeaders.HOST, GATEWAY_HOST);
//
//        // Preserve Authorization header if it exists
//        HttpHeaders headers = exchange.getRequest().getHeaders();
//        if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
//            String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
//            requestBuilder.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
//            log.info("Forwarding Authorization header: {}", authorizationHeader);
//        }
//
//        // Update the request with new headers
//        ServerHttpRequest updatedRequest = requestBuilder.build();
//
//        log.info("Updated URI for forwarding: {}", updatedRequest.getURI());
//        //BodyCaptureRequest requestDecorator = new BodyCaptureRequest(exchange.getRequest());
//        BodyCaptureResponse responseDecorator = new BodyCaptureResponse(exchange.getResponse());
//        ServerWebExchange mutatedExchange = exchange.mutate()
//                .request(updatedRequest)
//                .response(responseDecorator)
//                .build();
//
//        // Proceed with the modified exchange
//        return super.filter(mutatedExchange, chain)
//                .doOnSuccess(unused -> log.info("Successfully routed request for: {}", exchange.getRequest().getURI()))
//                .doOnError(error -> log.error("Load balancing failed: {}", error.getMessage()));
//
//
//
//        // Proceed with the modified exchange, capturing the response body
////        return super.filter(exchange.mutate().request(updatedRequest).build(), chain)
////                .then(Mono.defer(() -> {
////                    // Capture the response body
////                    Flux<DataBuffer> responseBody = exchange.getResponse().getBody();
////                    return responseBody.map(buffer -> {
////                        // Log the contents of the response buffer
////                        byte[] content = new byte[buffer.readableByteCount()];
////                        buffer.read(content);
////                        DataBufferUtils.release(buffer); // release buffer memory
////                        String responseBodyStr = new String(content, StandardCharsets.UTF_8);
////                        log.info("Response body: {}", responseBodyStr);
////                        return buffer;
////                    }).then();
////                }))
////                .doOnSuccess(unused -> log.info("Successfully routed request for: {}", updatedRequest.getURI()))
////                .doOnError(error -> log.error("Load balancing failed: {}", error.getMessage()));
//
//
//    }
//
//    private URI updateUriForLoadBalancing(ServerWebExchange exchange) {
//        URI originalUri = exchange.getRequest().getURI();
//        String serviceHost = "inventory-service";  // Replace with dynamic service selection if needed
//        int servicePort = 2222;  // Default port; customize as needed
//
//        // Construct URI pointing to the internal service through the load balancer
//        return URI.create(String.format("https://%s:%d%s", serviceHost, servicePort, originalUri.getPath()));
//    }
//
//    @Override
//    public int getOrder() {
//        return Ordered.HIGHEST_PRECEDENCE; // Ensure high priority in the filter chain
//    }
//}
