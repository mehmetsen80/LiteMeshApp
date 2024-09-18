package org.lite.gateway.config;

import lombok.RequiredArgsConstructor;
import org.lite.gateway.service.DynamicRouteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final DynamicRouteService dynamicRouteService;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity) {
        serverHttpSecurity
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .anyExchange()
                        .access(this::dynamicPathAuthorization)) // Dynamic authorization
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return serverHttpSecurity.build();
    }

    // Custom dynamic authorization logic based on dynamic route service
    private Mono<AuthorizationDecision> dynamicPathAuthorization(Mono<Authentication> authenticationMono, AuthorizationContext authorizationContext) {
        String path = authorizationContext.getExchange().getRequest().getPath().toString();

        // Use the path matcher from the DynamicRouteService to check if the path is whitelisted
        boolean isWhitelisted = dynamicRouteService.isPathWhitelisted(path);

        // Return authorization decision based on whitelist check
        return Mono.just(new AuthorizationDecision(isWhitelisted));
    }
}

