package org.lite.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.ServiceDTO;
import org.lite.gateway.repository.ApiRouteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRegistryService {

    private final ApiRouteRepository apiRouteRepository;
    private final HealthCheckService healthCheckService;

    public Flux<ServiceDTO> getAllServices() {
        return apiRouteRepository.findAll()
            .map(route -> {
                ServiceDTO.ServiceDTOBuilder builder = ServiceDTO.builder()
                    .serviceId(route.getRouteIdentifier())
                    .status("DOWN"); // Default status

                // Try to get health status if available
                healthCheckService.getServiceHealth(route.getRouteIdentifier())
                    .ifPresent(health -> {
                        builder
                            .status(health.getStatus())
                            .metrics(health.getMetrics())
                            .trends(health.getTrends())
                            .uptime(health.getUptime())
                            .lastChecked(health.getLastChecked());
                    });

                return builder.build();
            })
            .onErrorContinue((error, obj) -> {
                log.error("Error processing service: {}", error.getMessage());
            });
    }
} 