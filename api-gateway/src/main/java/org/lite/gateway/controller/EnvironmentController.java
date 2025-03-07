package org.lite.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/environment")
public class EnvironmentController {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @GetMapping("/profile")
    public Mono<EnvironmentInfo> getEnvironmentInfo() {
        return Mono.just(new EnvironmentInfo(activeProfile));
    }

    public record EnvironmentInfo(String profile) {}
}
