package org.lite.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.model.MetricAnalysis;
import org.lite.gateway.model.MetricPoint;
import org.lite.gateway.service.AdvancedMetricsAnalyzer;
import org.lite.gateway.service.MetricsAggregator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/analysis")
@Slf4j
public class MetricsAnalyzerController {
    
    private final AdvancedMetricsAnalyzer metricsAnalyzer;
    private final MetricsAggregator metricsAggregator;

    public MetricsAnalyzerController(
            AdvancedMetricsAnalyzer metricsAnalyzer,
            MetricsAggregator metricsAggregator) {
        this.metricsAnalyzer = metricsAnalyzer;
        this.metricsAggregator = metricsAggregator;
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<MetricAnalysis> getMetricAnalysis(
            @PathVariable String serviceId,
            @RequestParam String metric) {
        
        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceId);
        List<MetricPoint> points = history.getOrDefault(metric, Collections.emptyList());
        
        if (points.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MetricAnalysis analysis = metricsAnalyzer.analyzeMetric(points);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/{serviceId}/summary")
    public ResponseEntity<Map<String, MetricAnalysis>> getMetricsSummary(
            @PathVariable String serviceId) {
        
        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceId);
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, MetricAnalysis> summary = new HashMap<>();
        history.forEach((metric, points) -> {
            summary.put(metric, metricsAnalyzer.analyzeMetric(points));
        });

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{serviceId}/recommendations")
    public ResponseEntity<List<String>> getMetricRecommendations(
            @PathVariable String serviceId,
            @RequestParam String metric) {
        
        Map<String, List<MetricPoint>> history = metricsAggregator.getMetricsHistory(serviceId);
        List<MetricPoint> points = history.getOrDefault(metric, Collections.emptyList());
        
        if (points.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MetricAnalysis analysis = metricsAnalyzer.analyzeMetric(points);
        List<String> recommendations = analysis.getRecommendations();
        
        return ResponseEntity.ok(recommendations);
    }
} 