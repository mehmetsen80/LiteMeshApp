//package org.lite.gateway.config;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
//import org.springframework.security.oauth2.client.registration.ClientRegistration;
//import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
//import org.springframework.security.oauth2.core.OAuth2AccessToken;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Configuration
//@RequiredArgsConstructor
//public class GatewayGlobalFilterConfig {
//
//    private final OAuth2AuthorizedClientManager authorizedClientManager;
//    private final ReactiveClientRegistrationRepository clientRegistrationRepository;
//
//    @Bean
//    public GlobalFilter attachAccessTokenFilter() {
//        return (exchange, chain) -> {
//            return getClientAccessToken().flatMap(accessToken -> {
//                // Modify the request to include the Authorization header with the token
//                ServerWebExchange modifiedExchange = exchange.mutate()
//                        .request(request -> request.headers(headers -> headers.setBearerAuth(accessToken.getTokenValue())))
//                        .build();
//
//                return chain.filter(modifiedExchange);
//            });
//        };
//    }
//
//    private Mono<OAuth2AccessToken> getClientAccessToken() {
//        return clientRegistrationRepository.findByRegistrationId("lite-mesh-gateway-client")
//                .flatMap(clientRegistration -> {
//                    OAuth2AuthorizedClient client = authorizedClientManager.authorize(
//                            OAuth2AuthorizedClientProviderFactory.create(clientRegistration, clientRegistration.getClientId())
//                    );
//                    return Mono.justOrEmpty(client != null ? client.getAccessToken() : null);
//                });
//    }
//}
