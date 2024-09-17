package org.lite.gateway.config;

import lombok.RequiredArgsConstructor;
import org.lite.gateway.service.DynamicRouteService;
import org.lite.gateway.service.RouteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final RouteService routeService;

    private final DynamicRouteService dynamicRouteService;

    // Custom dynamic authorization logic based on dynamic route service
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity) {
        routeService.getAll()
                .collectList()
                .doOnSuccess(list -> {
                    list.forEach(apiRoute -> {
                        dynamicRouteService.addPath(apiRoute.path());
                    });
                    serverHttpSecurity
                            .csrf(ServerHttpSecurity.CsrfSpec::disable)
                            .authorizeExchange(exchange -> exchange
                                    .pathMatchers(dynamicRouteService.getWhitelistedPaths().toArray(String[]::new))
                                    .permitAll()
                                    .anyExchange()
                                    .authenticated())
                            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
                });
        return serverHttpSecurity.build();
    }
}

