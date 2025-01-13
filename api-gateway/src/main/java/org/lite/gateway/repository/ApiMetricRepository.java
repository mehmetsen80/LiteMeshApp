package org.lite.gateway.repository;

import org.lite.gateway.entity.ApiMetric;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import java.time.LocalDateTime;
import reactor.core.publisher.Flux;

public interface ApiMetricRepository extends ReactiveMongoRepository<ApiMetric, String> {
    @Query("{ " +
           "'timestamp': { $gte: ?0, $lte: ?1 }, " +
           "$and: [ " +
           "  { $or: [ { 'fromService': ?2 }, { $expr: { $eq: [?2, null] } } ] }, " +
           "  { $or: [ { 'toService': ?3 }, { $expr: { $eq: [?3, null] } } ] } " +
           "] }")
    Flux<ApiMetric> findByTimestampBetweenAndServices(
        LocalDateTime startDate,
        LocalDateTime endDate,
        String fromService,
        String toService
    );

    Flux<ApiMetric> findByFromServiceOrToService(String fromService, String toService);
}

