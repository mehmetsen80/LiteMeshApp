package org.lite.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.service.RouteService;
import org.lite.gateway.service.impl.ApiRouteLocatorImpl;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class GatewayConfiguration {

    @Bean
    public RouteLocator routeLocator(RouteService routeService, RouteLocatorBuilder routeLocationBuilder) {
        return new ApiRouteLocatorImpl(routeLocationBuilder, routeService);
    }
}
