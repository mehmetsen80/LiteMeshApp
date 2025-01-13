package org.lite.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiMetric;
import org.lite.gateway.repository.ApiMetricRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiMetricsService {

    // Define output type to avoid raw type warnings
    @SuppressWarnings("unchecked")
    private static final Class<Map<String, Object>> OUTPUT_TYPE = (Class<Map<String, Object>>) (Class<?>) Map.class;

    private final ApiMetricRepository apiMetricRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public Flux<ApiMetric> getMetrics(LocalDateTime startDate, LocalDateTime endDate, 
                                    String fromService, String toService) {
        return apiMetricRepository.findByTimestampBetweenAndServices(
            startDate != null ? startDate : LocalDateTime.MIN,
            endDate != null ? endDate : LocalDateTime.now(),
            fromService,
            toService
        );
    }

    public Mono<Map<String, Object>> getMetricsSummary(LocalDateTime startDate, LocalDateTime endDate) {
        Criteria timeCriteria = new Criteria();
        if (startDate != null && endDate != null) {
            timeCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);
        }

        AggregationOperation match = Aggregation.match(timeCriteria);
        AggregationOperation group = Aggregation.group()
                .count().as("totalRequests")
                .sum("duration").as("totalDuration")
                .avg("duration").as("avgDuration")
                .max("duration").as("maxDuration")
                .min("duration").as("minDuration")
                .sum(ConditionalOperators.when(Criteria.where("success").is(true))
                    .then(1)
                    .otherwise(0)).as("successfulRequests")
                .sum(ConditionalOperators.when(Criteria.where("success").is(false))
                    .then(1)
                    .otherwise(0)).as("failedRequests");

        TypedAggregation<ApiMetric> aggregation = Aggregation.newAggregation(ApiMetric.class, match, group);
        return reactiveMongoTemplate.aggregate(aggregation, OUTPUT_TYPE)
                .next()
                .defaultIfEmpty(new HashMap<>());
    }


    public Flux<Map<String, Object>> getServiceInteractions(LocalDateTime startDate, LocalDateTime endDate) {
        Criteria timeCriteria = new Criteria();
        if (startDate != null && endDate != null) {
            timeCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);
        }

        AggregationOperation match = Aggregation.match(timeCriteria);
        AggregationOperation group = Aggregation.group("fromService", "toService")
                .count().as("count")
                .avg("duration").as("avgDuration")
                .sum("duration").as("totalDuration")
                .sum(ConditionalOperators.when(Criteria.where("success").is(true))
                    .then(1)
                    .otherwise(0)).as("successCount")
                .sum(ConditionalOperators.when(Criteria.where("success").is(false))
                    .then(1)
                    .otherwise(0)).as("failureCount");

        AggregationOperation sort = Aggregation.sort(Sort.Direction.DESC, "count");

        TypedAggregation<ApiMetric> aggregation = Aggregation.newAggregation(ApiMetric.class, match, group, sort);
        return reactiveMongoTemplate.aggregate(aggregation, OUTPUT_TYPE);
    }

    public Flux<Map<String, Object>> getTopEndpoints(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        Criteria timeCriteria = new Criteria();
        if (startDate != null && endDate != null) {
            timeCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);
        }

        AggregationOperation match = Aggregation.match(timeCriteria);
        AggregationOperation group = Aggregation.group("pathEndPoint")
                .count().as("count")
                .avg("duration").as("avgDuration")
                .addToSet("toService").as("services");

        AggregationOperation sort = Aggregation.sort(Sort.Direction.DESC, "count");
        AggregationOperation limitOp = Aggregation.limit(limit);

        TypedAggregation<ApiMetric> aggregation = Aggregation.newAggregation(ApiMetric.class, match, group, sort, limitOp);
        return reactiveMongoTemplate.aggregate(aggregation, OUTPUT_TYPE);
    }

    public Mono<ApiMetric> getMetricById(String id) {
        return apiMetricRepository.findById(id);
    }

    // TODO: Implement secure delete operations
    /*
    public Mono<Void> deleteMetricById(String id) {
        return apiMetricRepository.deleteById(id);
    }

    public Mono<Void> deleteAllMetrics() {
        return apiMetricRepository.deleteAll();
    }
    */

    public Mono<Long> getMetricsCount() {
        return apiMetricRepository.count();
    }

    public Flux<ApiMetric> getMetricsByService(String serviceName) {
        return apiMetricRepository.findByFromServiceOrToService(serviceName, serviceName);
    }
} 