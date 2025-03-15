package org.lite.gateway.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.LinqRequest;
import org.lite.gateway.dto.LinqResponse;
import org.lite.gateway.service.LinqService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/linq")
@Slf4j
@AllArgsConstructor
public class LinqController {

    private final LinqService linqService;

    @PostMapping
    public Mono<LinqResponse> handleLinqRequest(@RequestBody LinqRequest request) {
        return linqService.processLinqRequest(request);
    }
}
