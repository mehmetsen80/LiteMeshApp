package org.lite.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class CircuitBreakerFallbackController {

    // i.e "fallbackUri": "/fallback/inventory" or fallbackUri": "/fallback/product" in mongodb
    // This handles fallback for any service that fails
    @GetMapping("/fallback/{serviceName}")
    public Mono<String> serviceFallback(@PathVariable String serviceName,
                                        @RequestParam(required = false) String exceptionMessage) {
        log.info("inside circuitbreaker");
        String fallbackMessage = "CircuitBreaker " + serviceName + " service is currently unavailable, please try again later.";
        if (exceptionMessage != null) {
            fallbackMessage += " Cause: " + exceptionMessage;
        }
        return Mono.just(fallbackMessage);
    }
}

