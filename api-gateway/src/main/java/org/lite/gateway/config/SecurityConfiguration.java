package org.lite.gateway.config;

import lombok.RequiredArgsConstructor;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.service.DynamicRouteService;
import org.lite.gateway.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final RouteService routeService;

    private final DynamicRouteService dynamicRouteService;

//    @Autowired
//    public SecurityConfiguration(DynamicRouteService dynamicRouteService) {
//        this.dynamicRouteService = dynamicRouteService;
//    }

    // Custom dynamic authorization logic based on dynamic route service
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity) {
        ;
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
                            //.access(this::dynamicPathAuthorization))// Dynamic authorization
                            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
                });
        //routeFlux.subscribe(f-> dynamicRouteService.addPath(f.path()));

        return serverHttpSecurity.build();
    }

//    private Mono<AuthorizationDecision> dynamicPathAuthorization(Mono<Authentication> authenticationMono, AuthorizationContext authorizationContext) {
//        String path = authorizationContext.getExchange().getRequest().getPath().toString();
//
//        // Check if the requested path is in the whitelist
//        boolean isWhitelisted = dynamicRouteService.getWhitelistedPaths()
//                .stream()
//                .anyMatch(path::startsWith);
//
//        // Return authorization decision based on the whitelist check
//        return Mono.just(new AuthorizationDecision(isWhitelisted));
//    }


//    @Bean
//    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity){
//        serverHttpSecurity
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                .authorizeExchange(exchange-> exchange
//                        .pathMatchers("/eureka/**", "/product/**", "/inventory/**", "/mesh/**", "routes/**")
//                        .permitAll()
//                        .anyExchange()
//                        .authenticated())
//                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
//
//        return serverHttpSecurity.build();
//
//    }
}

