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
import org.lite.gateway.entity.TeamRoute;
import org.lite.gateway.repository.TeamRouteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final ApiRouteRepository apiRouteRepository;
    private final ApiMetricRepository apiMetricRepository;
    private final TeamRouteRepository teamRouteRepository;
    
    @Override
    public Flux<StatDTO> getDashboardStats(String teamId) {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        
        return teamRouteRepository.findByTeamId(teamId)
            .map(TeamRoute::getRouteId)
            .collectList()
            .flatMapMany(routeIds -> Flux.merge(
                getActiveRoutesCount(routeIds),
                getAverageResponseTime(routeIds, cutoff),
                getRequestsPerMinute(routeIds, cutoff),
                getSuccessRate(routeIds, cutoff)
            ).doOnError(error -> {
                log.error("Error getting dashboard stats: ", error);
            }));
    }

    @Override
    public Flux<EndpointLatencyStats> getLatencyStats(String teamId, String timeRange) {
        Duration duration = parseDuration(timeRange);
        Instant cutoff = Instant.now().minus(duration);

        return teamRouteRepository.findByTeamId(teamId)
            .flatMap(teamRoute -> apiRouteRepository.findById(teamRoute.getRouteId())
                .map(ApiRoute::getRouteIdentifier))
            .collectList()
            .flatMap(routeIdentifiers -> checkMetricsExist(routeIdentifiers, cutoff)
                .thenReturn(routeIdentifiers))
            .flatMapMany(routeIdentifiers ->
                apiMetricRepository.getEndpointLatencyStats(routeIdentifiers, cutoff));
    }

    @Override
    public Flux<ServiceUsageStats> getServiceUsage(String teamId) {
        // First get the team's routes
        return teamRouteRepository.findByTeamId(teamId)
            .flatMap(teamRoute -> apiRouteRepository.findById(teamRoute.getRouteId()))
            .collectList()
            .flatMapMany(routes -> {
                if (routes.isEmpty()) {
                    log.warn("No routes found for team {}", teamId);
                    return Flux.empty();
                }

                // Extract route identifiers - these are already service names
                List<String> routeIdentifiers = routes.stream()
                    .map(ApiRoute::getRouteIdentifier)
                    .collect(Collectors.toList());

                // Get metrics aggregation and combine with all services
                return apiMetricRepository.getServiceUsageStats(routeIdentifiers)
                    .collectList()
                    .flatMapMany(metrics -> {
                        Map<String, Long> metricCounts = metrics.stream()
                            .collect(Collectors.toMap(
                                ServiceUsageAggregation::getId,
                                ServiceUsageAggregation::getRequestCount,
                                Long::sum
                            ));

                        // Create stats for all services, using 0 for those without metrics
                        return Flux.fromIterable(routeIdentifiers)
                            .map(service -> ServiceUsageStats.builder()
                                .service(service)
                                .requestCount(metricCounts.getOrDefault(service, 0L))
                                .build());
                    });
            })
            .doOnComplete(() -> log.info("Completed processing service usage stats for team {}", teamId))
            .onErrorResume(error -> {
                log.error("Error getting service usage stats for team {}: {}", teamId, error.getMessage());
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

    private Mono<StatDTO> getActiveRoutesCount(List<String> routeIds) {
        return apiRouteRepository.countByIdIn(routeIds)
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

    private Mono<StatDTO> getAverageResponseTime(List<String> routeIds, Instant cutoff) {
        return apiMetricRepository.getAverageResponseTime(routeIds, cutoff)
            .defaultIfEmpty(0.0)
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

    private Mono<StatDTO> getRequestsPerMinute(List<String> routeIds, Instant cutoff) {
        return apiMetricRepository.getRequestsPerMinute(routeIds, cutoff)
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

    private Mono<StatDTO> getSuccessRate(List<String> routeIds, Instant cutoff) {
        return apiMetricRepository.getSuccessRate(routeIds, cutoff)
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

    private Mono<Long> checkMetricsExist(List<String> routeIdentifiers, Instant cutoff) {
        return apiMetricRepository.countMetrics(routeIdentifiers, cutoff)
            .doOnNext(count -> {
                log.info("Found {} metrics for route identifiers {} since {}", 
                    count, routeIdentifiers, cutoff);
            });
    }
} 