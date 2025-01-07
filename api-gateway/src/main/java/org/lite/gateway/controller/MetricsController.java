package org.lite.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.model.MetricPoint;
import org.lite.gateway.service.MetricsAggregator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/metrics")
@Slf4j
public class MetricsController {
    
    private final MetricsAggregator metricsAggregator;

    public MetricsController(MetricsAggregator metricsAggregator) {
        this.metricsAggregator = metricsAggregator;
    }

    @GetMapping("/{serviceId}/current")
    public ResponseEntity<Map<String, Double>> getCurrentMetrics(@PathVariable String serviceId) {
        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceId);
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
    }

    @GetMapping("/{serviceId}/{metric}/history")
    public ResponseEntity<List<MetricPoint>> getMetricHistory(
            @PathVariable String serviceId,
            @PathVariable String metric) {
        
        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceId);
        List<MetricPoint> points = history.getOrDefault(metric, Collections.emptyList());
        
        if (points.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(points);
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<Map<String, List<MetricPoint>>> getAllMetrics(
            @PathVariable String serviceId) {
        
        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceId);
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(history);
    }
} 