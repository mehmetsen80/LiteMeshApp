package org.lite.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.model.MetricAnalysis;
import org.lite.gateway.service.AdvancedMetricsAnalyzer;
import org.lite.gateway.service.MetricsAggregator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/metrics/analysis")
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
    public Mono<ResponseEntity<MetricAnalysis>> getMetricAnalysis(
            @PathVariable String serviceId,
            @RequestParam String metric) {
        
        return Mono.just(metricsAggregator.getMetricsHistory(serviceId))
                .map(history -> history.getOrDefault(metric, Collections.emptyList()))
                .map(points -> {
                    if (points.isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }
                    MetricAnalysis analysis = metricsAnalyzer.analyzeMetric(points);
                    return ResponseEntity.ok(analysis);
                });
    }

    @GetMapping("/{serviceId}/summary")
    public Mono<ResponseEntity<Map<String, MetricAnalysis>>> getMetricsSummary(
            @PathVariable String serviceId) {
        
        return Mono.just(metricsAggregator.getMetricsHistory(serviceId))
                .map(history -> {
                    if (history.isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }

                    Map<String, MetricAnalysis> summary = new HashMap<>();
                    history.forEach((metric, points) -> {
                        summary.put(metric, metricsAnalyzer.analyzeMetric(points));
                    });

                    return ResponseEntity.ok(summary);
                });
    }

    @GetMapping("/{serviceId}/recommendations")
    public Mono<ResponseEntity<List<String>>> getMetricRecommendations(
            @PathVariable String serviceId,
            @RequestParam String metric) {
        
        return Mono.just(metricsAggregator.getMetricsHistory(serviceId))
                .map(history -> history.getOrDefault(metric, Collections.emptyList()))
                .map(points -> {
                    if (points.isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }

                    MetricAnalysis analysis = metricsAnalyzer.analyzeMetric(points);
                    List<String> recommendations = analysis.getRecommendations();
                    
                    return ResponseEntity.ok(recommendations);
                });
    }
} 