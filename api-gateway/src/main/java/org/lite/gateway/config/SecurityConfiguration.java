package org.lite.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.service.DynamicRouteService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.reactive.function.client.WebClient;
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
                //.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtSpec -> new KeycloakClientAuthoritiesConverter()))
                //.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
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

//    @Bean
//    public WebClient webClient(WebClient.Builder webClientBuilder) {
//        return webClientBuilder
//                .filter(new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager))
//                .build();
//    }

//    @Bean
//    public WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
//        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
//        return WebClient.builder().filter(oauth).build();
//    }

//    @Bean
//    public WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
//        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Client = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
//        oauth2Client.setDefaultClientRegistrationId("lite-mesh-gateway-client");  // The client registration ID configured in application.yml
//
//        return WebClient.builder()
//                .filter(oauth2Client)
//                .build();
//    }
//
//    @Bean
//    @LoadBalanced
//    public WebClient.Builder loadBalancedWebClientBuilder(ReactiveClientRegistrationRepository clientRegistrations,
//                                                          ObjectMapper objectMapper,
//                                                          ServerOAuth2AuthorizedClientRepository clientRepository) {
//
//        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2ClientFilter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
//                clientRegistrations,
//                clientRepository);
//        oauth2ClientFilter.setDefaultClientRegistrationId("lite-mesh-gateway-client");
//        WebClient.Builder builder = WebClient.builder();
//        builder.defaultHeader("Content-Type", MediaType.APPLICATION_JSON.toString());
//        builder.defaultHeader("Accept", MediaType.APPLICATION_JSON.toString());
//        builder.filter(oauth2ClientFilter);
//        return builder;
//    }

    @Bean
    public CommandLineRunner runAtStartup(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        return args -> {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("lite-mesh-gateway-client")
                    .principal("gateway-principal")  // Use a dummy principal for client_credentials
                    .build();

//            authorizedClientManager.authorize(authorizeRequest)
//                    .flatMap(authorizedClient -> {
//                        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
//                        if (accessToken != null) {
//                            log.info("Access Token: {}", accessToken.getTokenValue());
//                            return Mono.just(accessToken);
//                        } else {
//                            log.warn("No access token found!");
//                            return Mono.empty();
//                        }
//                    })
//                    .subscribe(token -> log.info("Token retrieved at startup: {}", token.getTokenValue()),
//                            error -> log.error("Failed to retrieve token at startup", error));


            // Attempt to authorize and retrieve token
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
                    .subscribe(token -> log.info("Token successfully retrieved at startup: {}", token.getTokenValue()),
                            error -> log.error("Failed to retrieve token at startup", error));


        };
    }

    // Bean to handle OAuth2 client credentials
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService) {

//        AuthorizationCodeReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = new AuthorizationCodeReactiveOAuth2AuthorizedClientProvider();
//        authorizedClientProvider.authorize()
        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .clientCredentials()
                .build();

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        // Log the token retrieval process for debugging purposes
//        authorizedClientManager.authorize(OAuth2AuthorizeRequest
//                .withClientRegistrationId("lite-mesh-gateway-client")
//                //.principal(Mono.just("gateway")) // Use a mock principal for testing
//                .principal("client")
//                .build()).flatMap(client -> {
//            OAuth2AccessToken accessToken = client.getAccessToken();
//            log.info("Retrieved access token: {}", accessToken.getTokenValue());
//            return Mono.just(client);
//        }).subscribe();

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

//    @Bean
//    @Order(Ordered.HIGHEST_PRECEDENCE)  // Set the order high to ensure it runs early
//    public GlobalFilter tokenRelayFilter(AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
//        return (exchange, chain) -> {
//            log.info("TokenRelayFilter applied to request: {}", exchange.getRequest().getURI());
//            // Token relay logic...
//            return null;
//        };
//    }

//    @Bean
//    public GlobalFilter tokenRelayFilter(AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
//        return (exchange, chain) -> {
//            // Simulate the principal for the client_credentials flow
//            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
//                    .withClientRegistrationId("lite-mesh-gateway-client") // The client registration ID for Keycloak
//                    .principal(createClientPrincipal()) // Create a principal representing the client
//                    .build();
//
//            // Log to ensure the filter is hit
//            log.info("Attempting to retrieve the token for client.");
//
//            return authorizedClientManager.authorize(authorizeRequest)
//                    .doOnNext(authorizedClient -> {
//                        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
//                            log.info("Successfully retrieved the token: {}", authorizedClient.getAccessToken().getTokenValue());
//                        } else {
//                            log.warn("Failed to retrieve the access token.");
//                        }
//                    })
//                    .flatMap(authorizedClient -> {
//                        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
//                            String accessToken = authorizedClient.getAccessToken().getTokenValue();
//                            // Add the token to the Authorization header
//                            exchange.getRequest().mutate().header("Authorization", "Bearer " + accessToken);
//                        }
//                        return chain.filter(exchange);
//                    })
//                    .onErrorResume(e -> {
//                        log.error("Error while retrieving the token", e);
//                        return chain.filter(exchange);
//                    });
//        };
//    }

//    private Authentication createClientPrincipal() {
//        // Creating a simple system principal representing the client
//        return new UsernamePasswordAuthenticationToken("client", "N/A", AuthorityUtils.NO_AUTHORITIES);
//    }




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

