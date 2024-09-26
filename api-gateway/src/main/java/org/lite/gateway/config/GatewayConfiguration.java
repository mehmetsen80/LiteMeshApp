package org.lite.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.service.RouteService;
import org.lite.gateway.service.impl.ApiRouteLocatorImpl;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.*;
//import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
//import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
//import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
//import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class GatewayConfiguration {
//
//    private final OAuth2AuthorizedClientManager authorizedClientManager;
//    private final ReactiveClientRegistrationRepository clientRegistrationRepository;

//    @Bean
//    public OAuth2AuthorizedClientManager authorizedClientManager(
//            ClientRegistrationRepository clientRegistrationRepository,
//            OAuth2AuthorizedClientRepository auth2AuthorizedClientRepository) {
//
//        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
//                .clientCredentials()  // Enables client credentials flow
//                .build();
//
//        DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
//                clientRegistrationRepository, auth2AuthorizedClientRepository);
//        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
//
//        return authorizedClientManager;
//    }

//    @Bean
//    ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager(
//            ReactiveClientRegistrationRepository clientRegistrationRepository,
//            ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
//
//        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
//                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
//                        .authorizationCode()
//                        .refreshToken()
//                        .clientCredentials()
//                        .build();
//
//        // Create the default reactive authorized client manager
//        DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(
//                clientRegistrationRepository, authorizedClientRepository);
//        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
//
//        // Configure to use ReactiveSecurityContextHolder for SecurityContext management
//        // We don't use any attributes in the keycloak, so it will never hit the block inside but keep this for the future
//        authorizedClientManager.setContextAttributesMapper(contextAttributesMapper -> Mono.defer(() -> ReactiveSecurityContextHolder.getContext()
//                .doOnNext(securityContext -> {
//                    // Add debugging log to ensure security context is being retrieved
//                    log.info("SecurityContext: {}", securityContext);
//                })
//                .map(securityContext -> {
//                    // Ensure the current security context is properly retrieved
//                    Authentication authentication = securityContext.getAuthentication();
//                    log.info("Authentication: {}", authentication);  // Check the authentication object
//                    return Collections.singletonMap(OAuth2AuthorizationContext.REQUEST_SCOPE_ATTRIBUTE_NAME, authentication);
//                })));
//
//        return authorizedClientManager;
//    }



//    // This method fetches the client credentials token and can be used in your custom filter
//    private Mono<OAuth2AccessToken> getClientAccessToken(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
//        return Mono.defer(() -> {
//            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("lite-mesh-gateway-client")
//                    .principal("lite-mesh-gateway-client") // The principal representing the client
//                    .build();
//            return Mono.justOrEmpty(authorizedClientManager.authorize(authorizeRequest))
//                    .map(OAuth2AuthorizedClient::getAccessToken);
//        });
//    }
//
//    @Bean
//    public GlobalFilter attachAccessTokenFilter(OAuth2AuthorizedClientManager authorizedClientManager) {
//        return (exchange, chain) -> {
//            return getClientAccessToken(authorizedClientManager).flatMap(accessToken -> {
//                // Modify the request to include the Authorization header with the token
//                ServerWebExchange modifiedExchange = exchange.mutate()
//                        .request(request -> request.headers(headers -> headers.setBearerAuth(accessToken.getTokenValue())))
//                        .build();
//
//                return chain.filter(modifiedExchange);
//            });
//        };
//    }

    @Bean
    public RouteLocator routeLocator(RouteService routeService, RouteLocatorBuilder routeLocationBuilder) {
        return new ApiRouteLocatorImpl(routeLocationBuilder, routeService);
    }
}
