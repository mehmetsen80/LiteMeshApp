package org.lite.gateway.filter;

import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class RedisRateLimiterFilterStrategy implements FilterStrategy{

    private final ApplicationContext applicationContext;

    public RedisRateLimiterFilterStrategy(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void apply(ApiRoute apiRoute, GatewayFilterSpec gatewayFilterSpec, FilterConfig filter) {
        // Extract rate limiter parameters from the filter config args
        int replenishRate = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("replenishRate")));
        int burstCapacity = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("burstCapacity")));
        int requestedTokens = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("requestedTokens")));

        // Configure RedisRateLimiter with extracted parameters
        RedisRateLimiter redisRateLimiter = new RedisRateLimiter(replenishRate, burstCapacity, requestedTokens);
        redisRateLimiter.setApplicationContext(applicationContext);
        gatewayFilterSpec.requestRateLimiter().configure(config -> {
            config.setRouteId(apiRoute.getRouteIdentifier());
            config.setRateLimiter(redisRateLimiter); // Set the RedisRateLimiter
            //config.setKeyResolver(exchange -> Mono.just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress())); // Use IP address as the key for limiting
            config.setKeyResolver(exchange -> Mono.just(Objects.requireNonNull(apiRoute.getRouteIdentifier())));
            config.setDenyEmptyKey(true); // Deny requests that have no resolved key
            config.setEmptyKeyStatus(HttpStatus.TOO_MANY_REQUESTS.name()); // Set response status to 429 when no key is resolved
        }).filter(new CustomRateLimitResponseFilter());
    }
}
