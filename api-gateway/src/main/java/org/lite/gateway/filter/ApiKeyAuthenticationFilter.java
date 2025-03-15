package org.lite.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.service.ApiKeyService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter implements WebFilter {
    private final ApiKeyService apiKeyService;
    private static final String API_KEY_HEADER = "X-API-Key";

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Skip API key for all non-linq
        if (!path.startsWith("/linq")) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        
        if (apiKey == null) {
            log.warn("No API key provided for path: {}", path);
            return Mono.error(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "API key is required"));
        }

        return apiKeyService.validateApiKey(apiKey)
            .map(validApiKey -> {
                log.debug("Valid API key for team: {} accessing: {}", 
                    validApiKey.getTeamId(), path);
                
                return new UsernamePasswordAuthenticationToken(
                    validApiKey.getTeamId(),
                    apiKey,
                    List.of(new SimpleGrantedAuthority("ROLE_API_ACCESS"))
                );
            })
            .flatMap(authentication -> 
                chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
            )
            .switchIfEmpty(Mono.error(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid API key")))
            .onErrorResume(ResponseStatusException.class, e -> {
                // Only handle the error if the response hasn't been committed
                if (!exchange.getResponse().isCommitted()) {
                    exchange.getResponse().setStatusCode(e.getStatusCode());
                    return exchange.getResponse().setComplete();
                }
                return Mono.empty();
            });
    }
} 