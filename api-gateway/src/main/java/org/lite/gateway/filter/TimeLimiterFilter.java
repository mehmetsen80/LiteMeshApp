package org.lite.gateway.filter;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.model.TimeLimiterRecord;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
                        log.error("Exception occurred in TimeLimiter: {}", e.getMessage());
                        throw new RuntimeException(e); //throw the actual error that occurred, not TimeoutException (this will jump to onErrorResume)
                    }
                })
                .flatMap(ignored -> Mono.empty()) // As we are dealing with `Void`, we just return an empty Mono on success
                .onErrorResume(throwable -> handleException(exchange, throwable))// Handle errors
                .then();
    }

    private Mono<Void> handleException(ServerWebExchange exchange, Throwable throwable) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            log.info("Propagating Authorization header after retries.");
            exchange.getRequest().mutate().header(HttpHeaders.AUTHORIZATION, headers.getFirst(HttpHeaders.AUTHORIZATION));
            log.info(exchange.getRequest().getHeaders().toString());
        }

        String errorMessage = "Unknown error occurred";

        if(throwable instanceof RuntimeException){
            throwable = throwable.getCause();
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR; // Default to 500 Internal Server Error
        errorMessage = throwable.getMessage() != null ? throwable.getMessage() : errorMessage;
        if (throwable instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT; // 504 Gateway Timeout
        } else if (throwable instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST; // 400 Bad Request
        } else if (throwable instanceof org.springframework.web.server.ResponseStatusException &&
                ((org.springframework.web.server.ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND) {
            status = HttpStatus.NOT_FOUND; // 404 Not Found
        } else if (errorMessage.contains("Unable to find instance")) {
            log.info("Inside Unable to find instance if statement");
            status = HttpStatus.SERVICE_UNAVAILABLE; // 503 Service Unavailable
        }
        log.error("Error handling in TimeLimiterFilter: {}, Status: {}", errorMessage, status);
        return writeResponse(exchange, status, errorMessage);
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
