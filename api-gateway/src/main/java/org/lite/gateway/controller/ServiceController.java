package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.ServiceDTO;
import org.lite.gateway.service.ServiceRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Slf4j
public class ServiceController {

    private final ServiceRegistryService serviceRegistryService;

    @GetMapping
    public ResponseEntity<Flux<ServiceDTO>> getAllServices() {
        log.debug("REST request to get all services");
        return ResponseEntity.ok(serviceRegistryService.getAllServices());
    }
} 