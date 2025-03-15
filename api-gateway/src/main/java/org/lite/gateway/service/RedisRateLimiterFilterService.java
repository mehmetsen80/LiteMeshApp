package org.lite.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.RedisRateLimiterFilter;
import org.lite.gateway.model.RedisRateLimiterRecord;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class RedisRateLimiterFilterService implements FilterService{

    private final ApplicationContext applicationContext;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRateLimiterFilterService(ApplicationContext applicationContext, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper){
        this.applicationContext = applicationContext;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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

            // Use the daily call limit if set
            Integer maxCallsPerDay = apiRoute.getMaxCallsPerDay();

            gatewayFilterSpec.requestRateLimiter().configure(config -> {
                config.setRouteId(apiRoute.getRouteIdentifier());
                config.setRateLimiter(redisRateLimiter);
                config.setKeyResolver(exchange -> Mono.just(apiRoute.getRouteIdentifier()));
                config.setDenyEmptyKey(true);
                config.setEmptyKeyStatus(HttpStatus.TOO_MANY_REQUESTS.name());
            })
                    .filter((exchange, chain) -> {
                        if (maxCallsPerDay != null) {
                            return handleDailyLimit(apiRoute, maxCallsPerDay, exchange)
                                    .flatMap(allowed -> allowed ? chain.filter(exchange) : tooManyRequestsResponse(exchange));
                        }
                        return chain.filter(exchange);
                    })
                    .filter(new RedisRateLimiterFilter(redisRateLimiterRecord));
        } catch (Exception e) {
            log.error("Error applying RedisRateLimiter filter for route {}: {}", apiRoute.getRouteIdentifier(), e.getMessage());
        }
    }

    private Mono<Boolean> handleDailyLimit(ApiRoute apiRoute, int maxCallsPerDay, ServerWebExchange exchange) {
        String dailyCallsKey = "dailyCalls:" + apiRoute.getPath(); // Use path or other identifier
        return Mono.fromCallable(() -> {
            Long currentCount = redisTemplate.opsForValue().increment(dailyCallsKey);
            if (currentCount == null || currentCount == 1) {
                currentCount = 1L;
                redisTemplate.expire(dailyCallsKey, Duration.ofDays(1)); // Expire after 24 hours
            }
            log.debug("API route {} has made {} calls today", apiRoute.getRouteIdentifier(), currentCount);
            return currentCount <= maxCallsPerDay;
        });
    }

    private Mono<Void> tooManyRequestsResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Custom message to return
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("error", "Too Many Requests");
        responseBody.put("message", "You have exceeded the maximum number of allowed requests. Please try again later.");
        responseBody.put("timestamp", String.valueOf(System.currentTimeMillis()));

        try {
            // Convert the response body to JSON
            String jsonResponse = objectMapper.writeValueAsString(responseBody);
            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            // Handle JSON conversion error
            return exchange.getResponse().setComplete();
        }
    }
}
