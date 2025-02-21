package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.lite.gateway.dto.EndpointLatencyStats;
import org.lite.gateway.dto.ServiceUsageAggregation;
import org.lite.gateway.dto.StatDTO;
import org.lite.gateway.dto.StatTrendDTO;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.dto.ServiceUsageStats;
import org.lite.gateway.service.DashboardService;
import org.lite.gateway.repository.ApiRouteRepository;
import org.lite.gateway.repository.ApiMetricRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final ApiRouteRepository apiRouteRepository;
    private final ApiMetricRepository apiMetricRepository;
    
    @Override
    public Flux<StatDTO> getDashboardStats() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        
        return Flux.merge(
            getActiveRoutes(),
            getResponseTime(cutoff),
            getRequestRate(cutoff),
            getSuccessRate(cutoff)
        ).doOnError(error -> {
            log.error("Error getting dashboard stats: ", error);
        });
    }

    @Override
    public Flux<EndpointLatencyStats> getLatencyStats(String timeRange) {
        Duration duration = parseDuration(timeRange);
        Instant cutoff = Instant.now().minus(duration);
        log.info("Fetching latency stats since {} (timeRange: {})", cutoff, timeRange);
        
        // Add a debug query to check data
        apiMetricRepository.findByTimestampAfter(cutoff)
            .doOnNext(metric -> {
                log.info("Found metric: service={}, path={}, duration={}, timestamp={}",
                    metric.getToService(),
                    metric.getPathEndPoint(),
                    metric.getDuration(),
                    metric.getTimestamp());
            })
            .subscribe();

        return apiMetricRepository.getEndpointLatencyStats(cutoff)
            .doOnNext(stats -> {
                log.info("Latency stats for {}: p50={}, p95={}, p99={}, count={}",
                    stats.getId().getPath(),
                    stats.getP50(),
                    stats.getP95(),
                    stats.getP99(),
                    stats.getCount());
            })
            .doOnComplete(() -> log.info("Completed fetching latency stats"))
            .doOnError(error -> {
                log.error("Error fetching latency stats: ", error);
            });
    }

    @Override
    public Flux<ServiceUsageStats> getServiceUsage() {
        // Get all unique service IDs from routes
        Flux<String> serviceIds = apiRouteRepository.findAll()
            .map(ApiRoute::getRouteIdentifier)
            .distinct();
        
        // Get metrics aggregation
        Flux<ServiceUsageAggregation> metrics = apiMetricRepository.getServiceUsageStats();
        
        // Combine both sources
        return serviceIds
            .map(serviceId -> {
                return metrics
                    .filter(m -> serviceId.equals(m.getId()))
                    .next()
                    .defaultIfEmpty(new ServiceUsageAggregation())
                    .map(metric -> ServiceUsageStats.builder()
                        .service(serviceId)
                        .requestCount(metric.getRequestCount() != null ? metric.getRequestCount() : 0L)
                        .build());
            })
            .flatMap(mono -> mono)
            .onErrorResume(error -> {
                log.error("Error getting service usage stats: ", error);
                return Flux.empty();
            });
    }

    private Duration parseDuration(String timeRange) {
        return switch (timeRange) {
            case "1h" -> Duration.ofHours(1);
            case "24h" -> Duration.ofHours(24);
            case "7d" -> Duration.ofDays(7);
            case "30d" -> Duration.ofDays(30);
            case "90d" -> Duration.ofDays(90);
            default -> Duration.ofHours(24); // Default to 24 hours
        };
    }

    private Mono<StatDTO> getActiveRoutes() {
        return apiRouteRepository.count()
            .map(count -> StatDTO.builder()
                .title("Active Routes")
                .value(String.valueOf(count))
                .type("count")
                .trend(StatTrendDTO.builder()
                    .percentChange(0.0)
                    .period("from last week")
                    .build())
                .build());
    }

    private Mono<StatDTO> getResponseTime(Instant cutoff) {
        return apiMetricRepository.getAverageResponseTime(cutoff)
            .defaultIfEmpty(0.0)  // Handle case when no metrics exist
            .map(avgTime -> StatDTO.builder()
                .title("Avg Response Time")
                .value(String.format("%.0f", avgTime))
                .type("time")
                .trend(StatTrendDTO.builder()
                    .percentChange(0.0)
                    .period("from last hour")
                    .build())
                .build());
    }

    private Mono<StatDTO> getRequestRate(Instant cutoff) {
        return apiMetricRepository.getRequestsPerMinute(cutoff)
            .defaultIfEmpty(0.0)
            .map(rate -> StatDTO.builder()
                .title("Requests/min")
                .value(String.format("%.0f", rate))
                .type("rate")
                .trend(StatTrendDTO.builder()
                    .percentChange(0.0)
                    .period("from last minute")
                    .build())
                .build());
    }

    private Mono<StatDTO> getSuccessRate(Instant cutoff) {
        return apiMetricRepository.getSuccessRate(cutoff)
            .defaultIfEmpty(1.0)
            .map(rate -> StatDTO.builder()
                .title("Success Rate")
                .value(String.format("%.1f", rate * 100))
                .type("percentage")
                .trend(StatTrendDTO.builder()
                    .percentChange(0.0)
                    .period("from last 5 minutes")
                    .build())
                .build());
    }
} 