package org.lite.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.service.DynamicRouteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

    private final DynamicRouteService dynamicRouteService;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity, AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        serverHttpSecurity
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterAt(tokenRelayWebFilter(authorizedClientManager), SecurityWebFiltersOrder.SECURITY_CONTEXT_SERVER_WEB_EXCHANGE)
                .authorizeExchange(exchange -> exchange
                        .anyExchange()
                        .access(this::dynamicPathAuthorization)); // Dynamic authorization
        return serverHttpSecurity.build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)  // Ensure this runs early
    public WebFilter tokenRelayWebFilter(AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        return (exchange, chain) -> {
            log.info("TokenRelayWebFilter applied to request: {}", exchange.getRequest().getURI());

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("lite-mesh-gateway-client")
                    .principal("lite-mesh-gateway-client")
                    .build();

            return authorizedClientManager.authorize(authorizeRequest)
                    .doOnError(error -> log.error("Error authorizing client: {}", error.getMessage()))
                    .flatMap(authorizedClient -> {
                        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                            String accessToken = authorizedClient.getAccessToken().getTokenValue();
                            log.info("Access Token retrieved: {}", accessToken);
                            exchange.getRequest().mutate().header("Authorization", "Bearer " + accessToken);
                        } else {
                            log.warn("No access token available for the client");
                        }
                        return chain.filter(exchange);
                    })
                    .onErrorResume(error -> {
                        log.error("Failed to authorize client: {}", error.getMessage());
                        return chain.filter(exchange);
                    });
        };
    }

    // Bean to handle OAuth2 client credentials
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService) {

        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .clientCredentials()
                .build();

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("lite-mesh-gateway-client")
                .principal("lite-mesh-gateway-client")  // Use a dummy principal for client_credentials
                .build();


        authorizedClientManager.authorize(authorizeRequest)
                .flatMap(authorizedClient -> {
                    if (authorizedClient != null) {
                        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                        if (accessToken != null) {
                            log.info("Access Token: {}", accessToken.getTokenValue());
                            return Mono.just(accessToken);
                        } else {
                            log.warn("Access token is null!");
                        }
                    } else {
                        log.warn("Authorized client is null!");
                    }
                    return Mono.empty();
                })
                .doOnError(error -> log.error("Error during token retrieval", error))
                .subscribe(token -> log.info("Token successfully retrieved at authorizedClientManager: {}", token.getTokenValue()),
                        error -> log.error("Failed to retrieve token at authorizedClientManager", error));

        return authorizedClientManager;
    }


    private Mono<AuthorizationDecision> dynamicPathAuthorization(Mono<Authentication> authenticationMono, AuthorizationContext authorizationContext) {
        String path = authorizationContext.getExchange().getRequest().getPath().toString();

        //1st step
        // Use the path matcher from the DynamicRouteService to check if the path is whitelisted.
        // whitelisted means either hard coded or read from mongodb
        boolean isWhitelisted = dynamicRouteService.isPathWhitelisted(path);

        //2nd step - check the realm access role
        //if whitelist passes, check for roles in the JWT token for secured paths
        if (isWhitelisted) {
            return authenticationMono
                    .doOnNext(auth -> log.info("Authentication object: " + auth))  // Debugging point
                    .doOnError(error -> log.error("Error in authenticationMono: {}", error.toString()))
                    .filter(authentication -> authentication instanceof JwtAuthenticationToken) // Ensure it's a JWT auth token
                    .cast(JwtAuthenticationToken.class)
                    .map(JwtAuthenticationToken::getToken)  // Get the JWT token
                    .map(jwt -> {
                        // Extract roles from JWT claims (adjust based on your Keycloak setup)
                        // Keycloak roles are often under 'realm_access' or 'resource_access'
                        var rolesRealm = jwt.getClaimAsMap("realm_access").get("roles");

                        // Ensure roles are a list and check for the specific role
                        if (rolesRealm instanceof List) {
                            // check realm role if authorized
                            boolean isAuthorizedRealm = ((List<String>) rolesRealm).contains("gateway_admin_realm");
                            if(isAuthorizedRealm){
                                // get the resource access map
                                var resourceAccess = jwt.getClaimAsMap("resource_access");
                                if (resourceAccess != null) {
                                    // get the client access
                                    // 3rd step - check out the client roles
                                    var clientAccess = resourceAccess.get("lite-mesh-gateway-client");
                                    if (clientAccess instanceof Map<?,?>) {
                                        // get the client roles
                                        var clientRoles = ((Map<String, Object>) clientAccess).get("roles");
                                        if (clientRoles instanceof List) {
                                            boolean isAuthorizedClient = ((List<String>) clientRoles).contains("gateway_admin");
                                            return new AuthorizationDecision(isAuthorizedClient); //finally return the authorization decision
                                        }
                                    }
                                }
                            }
                        }
                        return new AuthorizationDecision(false);
                    })
                    .defaultIfEmpty(new AuthorizationDecision(false)); // Default to unauthorized if no valid authentication
        }

        return Mono.just(new AuthorizationDecision(false));
    }
}

