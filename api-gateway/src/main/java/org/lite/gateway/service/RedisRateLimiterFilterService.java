package org.lite.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.RedisRateLimiterFilter;
import org.lite.gateway.model.RedisRateLimiterRecord;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
public class RedisRateLimiterFilterService implements FilterService{

    private final ApplicationContext applicationContext;

    public RedisRateLimiterFilterService(ApplicationContext applicationContext){
        this.applicationContext = applicationContext;
    }

    @Override
    public void applyFilter(GatewayFilterSpec gatewayFilterSpec, FilterConfig filter, ApiRoute apiRoute) {
        try {
            int replenishRate = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("replenishRate")));
            int burstCapacity = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("burstCapacity")));
            int requestedTokens = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("requestedTokens")));

            RedisRateLimiterRecord redisRateLimiterRecord = new RedisRateLimiterRecord(apiRoute.getRouteIdentifier(), replenishRate, burstCapacity, requestedTokens);
            RedisRateLimiter redisRateLimiter = new RedisRateLimiter(replenishRate, burstCapacity, requestedTokens);
            redisRateLimiter.setApplicationContext(applicationContext);

            gatewayFilterSpec.requestRateLimiter().configure(config -> {
                config.setRouteId(apiRoute.getRouteIdentifier());
                config.setRateLimiter(redisRateLimiter);
                config.setKeyResolver(exchange -> Mono.just(apiRoute.getRouteIdentifier()));
                config.setDenyEmptyKey(true);
                config.setEmptyKeyStatus(HttpStatus.TOO_MANY_REQUESTS.name());
            }).filter(new RedisRateLimiterFilter(redisRateLimiterRecord));
        } catch (Exception e) {
            log.error("Error applying RedisRateLimiter filter for route {}: {}", apiRoute.getRouteIdentifier(), e.getMessage());
        }
    }
}
