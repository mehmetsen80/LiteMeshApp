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
        return apiMetricRepository.save(metric).then();
    }
}

