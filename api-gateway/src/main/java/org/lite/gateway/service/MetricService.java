package org.lite.gateway.service;

import org.lite.gateway.entity.ApiMetric;
import org.lite.gateway.repository.ApiMetricRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MetricService {
    private final ApiMetricRepository apiMetricRepository;

    @Autowired
    public MetricService(ApiMetricRepository apiMetricRepository) {
        this.apiMetricRepository = apiMetricRepository;
    }

    public Mono<Void> saveMetric(ApiMetric metric) {
        // Skip saving if it's a health check request
        if (isHealthCheckRequest(metric)) {
            return Mono.empty();
        }
        return apiMetricRepository.save(metric).then();
    }

    private boolean isHealthCheckRequest(ApiMetric metric) {
        // Check if the pathEndpoint contains health endpoint
        return metric.getPathEndPoint() != null && 
               (metric.getPathEndPoint().endsWith("/health") || 
                metric.getPathEndPoint().endsWith("/health/"));
    }
}

