package org.lite.gateway.controller;

import lombok.extern.slf4j.Slf4j;

import org.lite.gateway.service.HealthCheckService;
import org.lite.gateway.service.MetricsAggregator;
import org.lite.gateway.model.TrendAnalysis;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import reactor.core.publisher.Mono;

import org.lite.gateway.model.DashboardUpdate;
import org.springframework.messaging.MessageDeliveryException;

@RestController
@RequestMapping("/health")
@Slf4j
public class HealthCheckServiceController {
    private final HealthCheckService healthCheckService;
    private final MetricsAggregator metricsAggregator;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ScheduledExecutorService scheduler;

    public HealthCheckServiceController(
            SimpMessagingTemplate simpMessagingTemplate,
            HealthCheckService healthCheckService,
            MetricsAggregator metricsAggregator) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.healthCheckService = healthCheckService;
        this.metricsAggregator = metricsAggregator;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startCheckUpdates();
    }

    // Send updates every 5 seconds
    private void startCheckUpdates() {
        scheduler.scheduleAtFixedRate(this::sendHealthUpdate, 0, 5, TimeUnit.SECONDS);
    }

    private void sendHealthUpdate() {
        healthCheckService.getHealthCheckEnabledRoutes()
            .flatMap(route -> healthCheckService.getServiceStatus(route.getRouteIdentifier())
                .map(status -> new DashboardUpdate(
                    route.getRouteIdentifier(),
                    status,
                    metricsAggregator.analyzeTrends(route.getRouteIdentifier())
                ))
                .onErrorResume(e -> {
                    log.error("Error getting status for {}: {}", route.getRouteIdentifier(), e.getMessage());
                    return Mono.empty();
                })
            )
            .collectList()
            .subscribe(this::sendHealthUpdate);
    }

    private void sendHealthUpdate(List<DashboardUpdate> payload) {
        try {
            log.info("sendHealthUpdate {}", payload);
            simpMessagingTemplate.convertAndSend("/topic/health", payload);
        } catch (MessageDeliveryException e) {
            log.warn("Failed to send health update: {}", e.getMessage());
            // Optionally implement retry logic here if needed
        }
    }

    // Triggered when a client subscribes
    @MessageMapping("/subscribe")
    public void handleSubscription() {
        sendHealthUpdate();
    }

    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
    }

    @GetMapping("/data/{serviceId}")
    public Mono<Map<String, Object>> getHealthData(@PathVariable String serviceId) {
        Map<String, Double> metrics = metricsAggregator.getCurrentMetrics(serviceId);

        // Get service health status
        Mono<Boolean> healthStatus = healthCheckService.isServiceHealthy(serviceId);
        
        // Get trends analysis
        Map<String, TrendAnalysis> trends = healthCheckService.analyzeServiceTrends(serviceId);

        // Combine all data
        return healthStatus.map(isHealthy -> {
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("serviceId", serviceId);
            dashboardData.put("healthy", isHealthy);
            dashboardData.put("metrics", metrics);
            dashboardData.put("trends", trends);
            return dashboardData;
        });
    }
} 