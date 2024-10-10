package org.lite.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class RetryFallbackController {

    // i.e "fallbackUri": "/retry/fallback/inventory" or fallbackUri": "/retry/fallback/product" in mongodb
    // This handles fallback for any service that fails
    @GetMapping("/retry/fallback/{serviceName}")
    public Mono<String> serviceFallback(@PathVariable String serviceName,
                                        @RequestParam(required = false) String exceptionMessage) {
        String fallbackMessage = "Retry: " + serviceName + " service is currently unavailable, please try again later.";
        if (exceptionMessage != null) {
            fallbackMessage += " Cause: " + exceptionMessage;
        }
        return Mono.just(fallbackMessage);
    }
}
