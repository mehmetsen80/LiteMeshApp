package org.lite.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.service.MetricService;
import org.lite.gateway.service.RouteService;
import org.lite.gateway.service.impl.ApiRouteLocatorImpl;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@Slf4j
public class GatewayConfig implements WebFluxConfigurer {
    
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder routeLocationBuilder,
                                    RouteService routeService,
                                    ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory,
                                    RedisTemplate<String, String> redisTemplate,
                                    MetricService metricService) {
        return new ApiRouteLocatorImpl(routeLocationBuilder,
                routeService, reactiveResilience4JCircuitBreakerFactory, redisTemplate, metricService);
    }

    @Bean
    public ServerCodecConfigurer serverCodecConfigurer() {
        return ServerCodecConfigurer.create();
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB
    }
}
