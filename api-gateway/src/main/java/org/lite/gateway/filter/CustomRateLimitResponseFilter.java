package org.lite.gateway.filter;

import com.nimbusds.jose.util.StandardCharset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CustomRateLimitResponseFilter implements GatewayFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
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
        return Ordered.HIGHEST_PRECEDENCE; // Ensures this filter runs after all others
    }
}

