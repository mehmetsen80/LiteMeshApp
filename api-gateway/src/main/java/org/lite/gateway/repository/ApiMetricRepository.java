package org.lite.gateway.repository;

import org.lite.gateway.entity.ApiMetric;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiMetricRepository extends ReactiveMongoRepository<ApiMetric, String> {
}

