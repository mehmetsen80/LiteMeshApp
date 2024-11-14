package org.lite.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.service.RouteService;
import org.lite.gateway.service.impl.ApiRouteLocatorImpl;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder routeLocationBuilder,
                                     RouteService routeService,
                                     ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory) {
        return new ApiRouteLocatorImpl(routeLocationBuilder,
                routeService, reactiveResilience4JCircuitBreakerFactory);
    }

//    @Bean
//    public GatewayFilterFactory<RewritePathGatewayFilterFactory.Config> rewritePathFilterFactory() {
//        return new RewritePathGatewayFilterFactory();
//    }

//    @Bean
//    public GatewayFilterFactory<LoadBalancerClientFilterFactory.Config> loadBalancerClientFilterFactory() {
//        return new LoadBalancerClientFilterFactory();
//    }

//    @Bean
//    public CustomReactiveLoadBalancerClientFilter customReactiveLoadBalancerClientFilter(
//            LoadBalancerClientFactory clientFactory,
//            GatewayLoadBalancerProperties properties) {
//        return new CustomReactiveLoadBalancerClientFilter(clientFactory, properties);
//    }
}
