package org.lite.gateway.service;

import org.lite.gateway.dto.StatDTO;
import org.lite.gateway.dto.EndpointLatencyStats;
import org.lite.gateway.dto.ServiceUsageStats;
import reactor.core.publisher.Flux;

public interface DashboardService {
    Flux<StatDTO> getDashboardStats(String teamId);
    Flux<EndpointLatencyStats> getLatencyStats(String teamId, String timeRange);
    Flux<ServiceUsageStats> getServiceUsage(String teamId);
} 