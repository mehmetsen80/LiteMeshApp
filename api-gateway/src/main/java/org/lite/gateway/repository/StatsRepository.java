package org.lite.gateway.repository;

import reactor.core.publisher.Mono;
import java.time.Duration;

public interface StatsRepository {
    Mono<Long> getRouteCountLastWeek();
    Mono<Double> getAverageResponseTime(Duration period);
    Mono<Double> getRequestsPerMinute(Duration period);
    Mono<Double> getSuccessRate(Duration period);
} 