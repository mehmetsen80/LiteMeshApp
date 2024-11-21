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
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final DynamicRouteService dynamicRouteService;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity, AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        serverHttpSecurity
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .x509(x509 -> x509
                        .principalExtractor(principal -> {
                            // Extract the CN from the certificate (adjust this logic as needed)
                            String dn = principal.getSubjectX500Principal().getName();
                            log.info("dn: {}", dn);
                            String cn = dn.split(",")[0].replace("CN=", "");
                            return cn;  // Return the Common Name (CN) as the principal
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .authorizeExchange(exchange -> exchange
                        .anyExchange()
                        .access(this::dynamicPathAuthorization))
                .addFilterAt(tokenRelayWebFilter(authorizedClientManager), SecurityWebFiltersOrder.SECURITY_CONTEXT_SERVER_WEB_EXCHANGE); // Dynamic authorization
        return serverHttpSecurity.build();
    }

    // Define a ReactiveUserDetailsService to map certificates to users
    // Do not remove this, although it might seem it's not being used
    // WebFluxSecurityConfiguration requires a bean of type 'org.springframework.security.core.userdetails.ReactiveUserDetailsService'
    @Bean
    public ReactiveUserDetailsService userDetailsService() {
        // Example: Hardcoded user with role
        UserDetails user = User.withUsername("example-cn")
                .password("{noop}password")  // Password is not used in mTLS
                .roles("USER", "ADMIN")
                .build();

        // A Map-based user details service
        return new MapReactiveUserDetailsService(user);
    }

    //We are injecting the gateway token here
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
                            String gatewayToken = authorizedClient.getAccessToken().getTokenValue();
                            log.info("Gateway Token retrieved: {}", gatewayToken);
                            // Forward the gateway's token to the downstream service
                            exchange.getRequest().mutate().header("Authorization", "Bearer " + gatewayToken);
                        } else {
                            //log.warn("No access token available for the client");
                            log.warn("No access token available for the gateway");
                        }
                        return chain.filter(exchange);
                    })
                    .onErrorResume(error -> {
                        log.error("Failed to authorize client: {}", error.getMessage());
                        return chain.filter(exchange); // Continue the filter chain even if token acquisition fails
                    });
        };
    }

    // Bean to handle OAuth2 client credentials
    // DO NOT DELETE THIS
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

//        //This is just to test if we get the token or not, enable this to test
//        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("lite-mesh-gateway-client")
//                .principal("lite-mesh-gateway-client")  // Use a dummy principal for client_credentials
//                .build();
//        authorizedClientManager.authorize(authorizeRequest)
//                .flatMap(authorizedClient -> {
//                    if (authorizedClient != null) {
//                        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
//                        if (accessToken != null) {
//                            log.info("Access Token: {}", accessToken.getTokenValue());
//                            // Log scopes (from token response)
//                            String scopes = String.join(",", authorizedClient.getAccessToken().getScopes());
//                            log.info("Granted Scopes: {}", scopes);
//
//                            return Mono.just(accessToken);
//                        } else {
//                            log.warn("Access token is null!");
//                        }
//                    } else {
//                        log.warn("Authorized client is null!");
//                    }
//                    return Mono.empty();
//                })
//                .doOnError(error -> log.error("Error during token retrieval", error))
//                .subscribe(token -> log.info("Token successfully retrieved at authorizedClientManager: {}", token.getTokenValue()),
//                        error -> log.error("Failed to retrieve token at authorizedClientManager", error));

        return authorizedClientManager;
    }

    private  String getScopeKey(String path) {
        // Regular expression to capture the dynamic prefix (e.g., "/inventory/", "/product/")
        Pattern pattern = Pattern.compile("^/(\\w+)/");
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            // Extract the prefix (e.g., "/inventory/")
            String prefix = matcher.group(0); // Full match, e.g., "/inventory/"
            return prefix + "**";
        }

        // If no matching prefix, return original path with "**"
        return path + "**";
    }

    private Mono<AuthorizationDecision> dynamicPathAuthorization(Mono<Authentication> authenticationMono, AuthorizationContext authorizationContext) {
        String path = authorizationContext.getExchange().getRequest().getPath().toString();
        log.info("authorizationContext.getExchange().getRequest().getPath(): {}", path);

        //1st step
        // Use the path matcher from the DynamicRouteService to check if the path is whitelisted.
        // whitelisted means either hard coded or read from mongodb
        boolean isWhitelisted = dynamicRouteService.isPathWhitelisted(path);

        String prefix = "/inventory/";
        String scopeKey = "";
        if (path.startsWith(prefix)) {
            scopeKey = prefix + "**";
        }
        String scope = dynamicRouteService.getClientScope(getScopeKey(path));//i.e. inventory/** -> inventory-service.read

        //2nd step - check the realm access role
        //if whitelist passes, check for roles in the JWT token for secured paths
        if (isWhitelisted) {
            return authenticationMono
                    .doOnNext(auth -> log.info("Evaluating JWT for path: {}", path))
                    .doOnError(error -> log.error("Error in authenticationMono: {}", error.toString()))
                    .filter(authentication -> authentication instanceof JwtAuthenticationToken) // Ensure it's a JWT auth token
                    .cast(JwtAuthenticationToken.class)
                    .map(JwtAuthenticationToken::getToken)  // Get the JWT token
                    .map(jwt -> {
                        String scopes = jwt.getClaimAsString("scope");
                        log.info("scopes: {}", scopes);
                        boolean hasGatewayReadScope = scopes != null && scopes.contains("gateway.read");//does the gateway itself has this scope
                        if (!hasGatewayReadScope) {
                            log.warn("JWT is missing 'gateway.read' scope");
                            return new AuthorizationDecision(false); // Deny if the hard-coded scope is missing
                        }

                        //i.e. inventory-service.read or product-service.read should be defined in the keycloak
                        //we bypass the refresh and fallback end points
                        if(!path.equals("/routes/refresh/routes") && !path.startsWith("/fallback/")){
                            boolean hasClientReadScope = scope != null && scopes.contains(scope);//does the client itself has the scope
                            if (!hasClientReadScope){
                                if(scope == null) {
                                    log.error("Client path {} is missing scope in mongodb and keycloak", path);
                                } else {
                                    log.error("Client path {} is missing scope {} in keycloak", path, scope);
                                }
                                return new AuthorizationDecision(false); // Deny if the client scope is missing
                            }
                        }

                        // Extract roles from JWT claims (adjust based on your Keycloak setup)
                        // Keycloak roles are often under 'realm_access' or 'resource_access'
                        // Extract Keycloak roles
                        var realmAccess = jwt.getClaimAsMap("realm_access");
                        var rolesRealm = realmAccess != null ? realmAccess.get("roles") : null;

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

