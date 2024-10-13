package org.lite.gateway.filter;

import com.nimbusds.jose.util.StandardCharset;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.model.RedisRateLimiterRecord;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
public class RedisRateLimiterFilter implements GatewayFilter, Ordered {

    private final RedisRateLimiterRecord redisRateLimiterRecord;

    public RedisRateLimiterFilter(RedisRateLimiterRecord redisRateLimiterRecord){
        this.redisRateLimiterRecord = redisRateLimiterRecord;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        log.info("Apply the RedisRateLimiter filter {}", redisRateLimiterRecord.toString());
        return chain.filter(exchange).then(Mono.defer(() -> {
            // Check if response status is 429 (TOO_MANY_REQUESTS)
            if (exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                // Log the rate limit exceeded event
                log.info("Custom RateLimiter triggered: 429 TOO_MANY_REQUESTS");

                // Customize the response for rate limit exceeded
                String responseMessage = "Rate limit exceeded. Please try again later.";
                DataBuffer buffer = exchange.getResponse().bufferFactory()
                        .wrap(responseMessage.getBytes(StandardCharset.UTF_8));

                // Write custom message to the response body
                return exchange.getResponse().writeWith(Mono.just(buffer));
            }
            return Mono.empty();
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
