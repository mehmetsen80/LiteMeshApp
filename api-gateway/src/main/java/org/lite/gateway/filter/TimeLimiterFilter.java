package org.lite.gateway.filter;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.model.TimeLimiterRecord;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TimeLimiterFilter implements GatewayFilter, Ordered {

    private final TimeLimiterRecord timeLimiterRecord;

    public TimeLimiterFilter(TimeLimiterRecord timeLimiterRecord) {
        this.timeLimiterRecord = timeLimiterRecord;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // Create the TimeLimiterConfig
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(timeLimiterRecord.timeoutDuration())) // Set the timeout duration
                .cancelRunningFuture(timeLimiterRecord.cancelRunningFuture()) // Whether to cancel running future on timeout
                .build();

        log.info("Configuring TimeLimiter for route: {}, timeoutDuration: {}, cancelRunningFuture: {}",
                timeLimiterRecord.routeId(), timeLimiterRecord.timeoutDuration(), timeLimiterRecord.cancelRunningFuture());
        // Create the TimeLimiter using the Resilience4J factory
        TimeLimiter timeLimiter = TimeLimiter.of("timeLimiter-" + timeLimiterRecord.routeId(), timeLimiterConfig);


        // Apply the TimeLimiter to the route
            log.info("Applying TimeLimiter for route: {}", timeLimiterRecord.routeId());

            // Convert Mono to CompletableFuture and apply the TimeLimiter
            CompletableFuture<Void> future = Mono.from(chain.filter(exchange))
                    .toFuture();

            // Apply the TimeLimiter logic
            return Mono.fromSupplier(() -> {
                        try {
                            return timeLimiter.executeFutureSupplier(() -> future);
                        } catch (Exception e) {
                            if (e instanceof TimeoutException) {
                                log.error("TimeoutException occurred: {}", e.getMessage());
                                throw new RuntimeException(new TimeoutException("Request timed out after " + timeLimiterRecord.timeoutDuration() + " seconds"));
                            }
                            throw new RuntimeException(e);
                        }
                    })
                    .flatMap(ignored -> Mono.empty()) // As we are dealing with `Void`, we just return an empty Mono on success
                    .onErrorResume(throwable -> {
                        if (throwable instanceof TimeoutException) {
                            log.info("Timeout occurred. Applying fallback logic for {}", timeLimiterRecord.routeId());
                            exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
                            String responseMessage = "Request timed out. Please try again later.";
                            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseMessage.getBytes(StandardCharsets.UTF_8));
                            return exchange.getResponse().writeWith(Mono.just(buffer));
                        }
                        return Mono.error(throwable); // Propagate other exceptions
                    })
                    .then();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }
}
