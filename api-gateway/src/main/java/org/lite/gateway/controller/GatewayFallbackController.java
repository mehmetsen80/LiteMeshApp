package org.lite.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class GatewayFallbackController {

    // i.e "fallbackUri": "/fallback/inventory" or fallbackUri": "/fallback/product" in mongodb
    @GetMapping("/fallback/{serviceName}")
    public Mono<String> serviceFallback(@PathVariable String serviceName) {
        return Mono.just(serviceName + " service is currently unavailable, please try again later.");
    }
}

