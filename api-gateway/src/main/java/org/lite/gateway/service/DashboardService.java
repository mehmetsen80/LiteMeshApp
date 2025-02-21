package org.lite.gateway.service;

import org.lite.gateway.dto.StatDTO;
import org.lite.gateway.dto.EndpointLatencyStats;
import org.lite.gateway.dto.ServiceUsageStats;
import reactor.core.publisher.Flux;

public interface DashboardService {
    Flux<StatDTO> getDashboardStats();
    Flux<EndpointLatencyStats> getLatencyStats(String timeRange);
    Flux<ServiceUsageStats> getServiceUsage();
} 