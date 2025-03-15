package org.lite.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.lite.gateway.dto.EndpointLatencyStats;
import org.lite.gateway.dto.ServiceUsageStats;
import org.lite.gateway.dto.StatDTO;
import org.lite.gateway.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Flux<StatDTO> getDashboardStats(@RequestParam String teamId) {
        return dashboardService.getDashboardStats(teamId);
    }

    @GetMapping("/latency")
    public Flux<EndpointLatencyStats> getLatencyStats(
        @RequestParam String teamId,
        @RequestParam(defaultValue = "30d") String timeRange
    ) {
        return dashboardService.getLatencyStats(teamId, timeRange);
    }

    @GetMapping("/service-usage")
    public Flux<ServiceUsageStats> getServiceUsage(@RequestParam String teamId) {
        return dashboardService.getServiceUsage(teamId);
    }
} 