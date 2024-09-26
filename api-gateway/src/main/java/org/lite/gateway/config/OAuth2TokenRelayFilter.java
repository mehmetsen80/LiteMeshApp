//package org.lite.gateway.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
//import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
//import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
//import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.security.oauth2.core.OAuth2AccessToken;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Component
//@Slf4j
//public class OAuth2TokenRelayFilter implements GlobalFilter, Ordered {
//
////    private final AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager;
////
////    public OAuth2TokenRelayFilter(AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
////        this.authorizedClientManager = authorizedClientManager;
////    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // Log entry to ensure the filter is being invoked
//        log.info("OAuth2TokenRelayFilter invoked for request path: " + exchange.getRequest().getPath());
//
//        return exchange.getPrincipal() // Check the principal and attempt to obtain the token
//                .cast(OAuth2AuthenticationToken.class)
//                .flatMap(authentication -> {
//                    // Build OAuth2AuthorizeRequest using the principal
//                    OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
//                            .withClientRegistrationId("lite-mesh-gateway-client") // use your client registration ID
//                            .principal(authentication)
//                            .build();
//
////                    return authorizedClientManager.authorize(authorizeRequest)
////                            .flatMap(authorizedClient -> {
////                                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
////                                log.info("Retrieved access token: {}", accessToken.getTokenValue());
////
////                                // Add the token to the request headers
////                                exchange.getRequest().mutate()
////                                        .header("Authorization", "Bearer " + accessToken.getTokenValue());
////
////                                return chain.filter(exchange);
////                            });
//
//                    return chain.filter(exchange);//TODO: remove this later
//
//                })
//                .switchIfEmpty(chain.filter(exchange)); // Continue the chain if no token is available
//    }
//
//    @Override
//    public int getOrder() {
//        return Ordered.HIGHEST_PRECEDENCE; // Order this filter to run before others
//    }
//}
