package org.lite.gateway.filter;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TimeLimiterFilterStrategy implements FilterStrategy{

    @Override
    public void apply(ApiRoute apiRoute, GatewayFilterSpec gatewayFilterSpec, FilterConfig filter) {
        // Extract TimeLimiter parameters from the filter config args
        int timeoutDuration = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("timeoutDuration")));
        boolean cancelRunningFuture = Boolean.parseBoolean(Objects.requireNonNull(filter.getArgs().get("cancelRunningFuture")));

        // Create the TimeLimiterConfig
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5)) // Set the timeout duration
                .cancelRunningFuture(cancelRunningFuture) // Whether to cancel running future on timeout
                .build();

        log.info("Configuring TimeLimiter for route: {}, timeoutDuration: {}, cancelRunningFuture: {}",
                apiRoute.getRouteIdentifier(), timeoutDuration, cancelRunningFuture);

        // Create the TimeLimiter using the Resilience4J factory
        TimeLimiter timeLimiter = TimeLimiter.of("timeLimiter-" + apiRoute.getRouteIdentifier(), timeLimiterConfig);

        // Apply the TimeLimiter to the route
        gatewayFilterSpec.filter((exchange, chain) -> {
            log.info("Applying TimeLimiter for route: {}", apiRoute.getRouteIdentifier());

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
                                throw new RuntimeException(new TimeoutException("Request timed out after " + timeoutDuration + " seconds"));
                            }
                            throw new RuntimeException(e);
                        }
                    })
                    .flatMap(ignored -> Mono.empty()) // As we are dealing with `Void`, we just return an empty Mono on success
                    .onErrorResume(throwable -> {
                        if (throwable instanceof TimeoutException) {
                            log.info("Timeout occurred. Applying fallback logic for {}", apiRoute.getRouteIdentifier());
                            exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
                            String responseMessage = "Request timed out. Please try again later.";
                            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseMessage.getBytes(StandardCharsets.UTF_8));
                            return exchange.getResponse().writeWith(Mono.just(buffer));
                        }
                        return Mono.error(throwable); // Propagate other exceptions
                    })
                    .then();
        });
    }
}
