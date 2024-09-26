//package org.lite.gateway.config;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.ReactiveSecurityContextHolder;
//import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
//import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.core.Ordered;
//import reactor.core.publisher.Mono;
//import org.springframework.web.server.WebFilterChain;
//
//@Slf4j
//@Component
////@RequiredArgsConstructor
//public class OAuth2GlobalFilter implements WebFilter, Ordered {
//
//    //private final WebClient webClient;  // WebClient that already has OAuth2 configuration
//
////    @Override
////    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
////        String originalUri = exchange.getRequest().getURI().toString();
////
////        // 1. Log the original request URI for debugging
////        log.info("Original Request URI: {}", originalUri);
////
////        // 2. Dynamically forward the request with the OAuth2 token
////        return webClient
////                .get()
////                .uri(originalUri)  // Use the original URI for forwarding the request
////                .retrieve()
////                .bodyToMono(String.class)
////                .flatMap(responseBody -> {
////                    // 3. Replace the response body in the exchange's response
////                    exchange.getResponse().getHeaders().add("Content-Type", "application/json");
////                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
////                            .bufferFactory()
////                            .wrap(responseBody.getBytes())));
////                });
////    }
//
//
//    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
//
//    public OAuth2GlobalFilter(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
//        this.authorizedClientManager = authorizedClientManager;
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//
//        return ReactiveSecurityContextHolder.getContext()
//                .flatMap(securityContext -> {
//                    // Extract the current authentication
//                    Authentication authentication = securityContext.getAuthentication();
//
//                    // Build the OAuth2AuthorizeRequest using the current authentication
//                    OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
//                            .withClientRegistrationId("lite-mesh-gateway-client")
//                            .principal(authentication)  // Pass the current authentication
//                            .build();
//
//                    // Authorize the client and obtain the token
//                    return authorizedClientManager.authorize(authorizeRequest)
//                            .flatMap(authorizedClient -> {
//                                // Attach the token to the current exchange or request headers as needed
//                                String token = authorizedClient.getAccessToken().getTokenValue();
//                                exchange.getRequest().mutate()
//                                        .header("Authorization", "Bearer " + token)
//                                        .build();
//                                return chain.filter(exchange);
//                            });
//                })
//                .switchIfEmpty(chain.filter(exchange));  // Continue the chain if no SecurityContext is available
//    }
//
//
//    @Override
//    public int getOrder() {
//        return Ordered.LOWEST_PRECEDENCE; // Ensures the filter runs early
//    }
//}