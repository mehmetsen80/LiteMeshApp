package org.lite.gateway.controller;

import org.lite.gateway.model.MetricPoint;
import org.lite.gateway.service.MetricsAggregator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/metrics/points")
@Slf4j
public class MetricsPointController {
    
    private final MetricsAggregator metricsAggregator;

    public MetricsPointController(MetricsAggregator metricsAggregator) {
        this.metricsAggregator = metricsAggregator;
    }

    @GetMapping("/{serviceId}/current")
    public Mono<ResponseEntity<Map<String, Double>>> getCurrentMetrics(@PathVariable String serviceId) {
        return Mono.just(metricsAggregator.getMetricsHistory(serviceId))
                .map(history -> {
                    if (history.isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }

                    Map<String, Double> currentMetrics = new HashMap<>();
                    history.forEach((metric, points) -> {
                        if (!points.isEmpty()) {
                            currentMetrics.put(metric, points.getLast().getValue());
                        }
                    });

                    return ResponseEntity.ok(currentMetrics);
                });
    }

    @GetMapping("/{serviceId}/{metric}/history")
    public Mono<ResponseEntity<List<MetricPoint>>> getMetricHistory(
            @PathVariable String serviceId,
            @PathVariable String metric) {
        
        return Mono.just(metricsAggregator.getMetricsHistory(serviceId))
                .map(history -> {
                    List<MetricPoint> points = history.getOrDefault(metric, Collections.emptyList());
                    if (points.isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(points);
                });
    }

    @GetMapping("/{serviceId}")
    public Mono<ResponseEntity<Map<String, List<MetricPoint>>>> getAllMetrics(
            @PathVariable String serviceId) {
        
        return Mono.just(metricsAggregator.getMetricsHistory(serviceId))
                .map(history -> {
                    if (history.isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(history);
                });
    }
} 