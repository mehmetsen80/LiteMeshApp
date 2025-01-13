package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.lite.gateway.entity.ApiMetric;
import org.lite.gateway.service.ApiMetricsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Slf4j
public class ApiMetricsController {

    private final ApiMetricsService apiMetricsService;

    @GetMapping
    public Flux<ApiMetric> getMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String fromService,
            @RequestParam(required = false) String toService) {
        
        // If we're filtering by service but no dates provided, don't restrict the date range
        if ((fromService != null || toService != null) && startDate == null && endDate == null) {
            startDate = LocalDateTime.MIN;
            endDate = LocalDateTime.now();
        } else {
            // Only apply default date range if no filters are provided at all
            if (startDate == null) {
                startDate = LocalDateTime.now().minusMonths(1);
            }
            if (endDate == null) {
                endDate = LocalDateTime.now();
            }
        }

        // Validate date range
        if (startDate.isAfter(endDate)) {
            return Flux.error(new IllegalArgumentException("Start date must be before end date"));
        }

        log.debug("Fetching metrics - startDate: {}, endDate: {}, fromService: {}, toService: {}",
                 startDate, endDate, fromService, toService);

        try {
            return apiMetricsService.getMetrics(startDate, endDate, fromService, toService)
                .doOnError(error -> {
                    log.error("Error fetching metrics: {}", error.getMessage(), error);
                });
        } catch (Exception e) {
            log.error("Unexpected error in getMetrics: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }

    @GetMapping("/summary")
    public Mono<Map<String, Object>> getMetricsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        return apiMetricsService.getMetricsSummary(startDate, endDate);
    }

    @GetMapping("/service-interactions")
    public Flux<Map<String, Object>> getServiceInteractions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        return apiMetricsService.getServiceInteractions(startDate, endDate);
    }

    @GetMapping("/top-endpoints")
    public Flux<Map<String, Object>> getTopEndpoints(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") int limit) {
        
        return apiMetricsService.getTopEndpoints(startDate, endDate, limit);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiMetric>> getMetricById(@PathVariable String id) {
        return apiMetricsService.getMetricById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // TODO: Implement secure delete operations
    /*
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteMetricById(@PathVariable String id) {
        // TODO: Add role-based security check (e.g., ADMIN or METRICS_MANAGER role required)
        return apiMetricsService.deleteMetricById(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping
    public Mono<ResponseEntity<Void>> deleteAllMetrics() {
        // TODO: Add role-based security check (e.g., ADMIN role required)
        // This is a destructive operation and should be highly restricted
        return apiMetricsService.deleteAllMetrics()
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
    */

    @GetMapping("/count")
    public Mono<Long> getMetricsCount() {
        return apiMetricsService.getMetricsCount();
    }

    @GetMapping("/service/{serviceName}")
    public Flux<ApiMetric> getMetricsByService(@PathVariable String serviceName) {
        return apiMetricsService.getMetricsByService(serviceName);
    }
} 