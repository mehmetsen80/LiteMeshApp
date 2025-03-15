package org.lite.gateway.repository;

import org.lite.gateway.dto.ErrorStats;
import org.lite.gateway.dto.RouteStats;
import org.lite.gateway.dto.ServiceErrorStats;
import org.lite.gateway.dto.ServiceErrorTrend;
import org.lite.gateway.dto.ServiceStats;
import org.lite.gateway.dto.ServiceTimeSeriesStats;
import org.lite.gateway.dto.ServiceUsageAggregation;
import org.lite.gateway.dto.TimeSeriesStats;
import org.lite.gateway.dto.EndpointStats;
import org.lite.gateway.dto.EndpointTimeSeriesStats;
import org.lite.gateway.dto.EndpointLatencyStats;
import org.lite.gateway.entity.ApiMetric;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    @Query("{ 'timestamp': { $gte: ?0 } }")
    Mono<Long> countRequestsAfter(Instant cutoff);

    @Query("{ 'timestamp': { $gte: ?0 }, 'statusCode': { $gte: 200, $lt: 300 } }")
    Mono<Long> countSuccessfulRequestsAfter(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { " +
        "    routeIdentifier: { $in: ?0 }, " +
        "    timestamp: { $gte: ?1 } " +
        "} }",
        "{ $group: { _id: null, avgDuration: { $avg: '$duration' } } }",
        "{ $project: { _id: 0, avgDuration: 1 } }"
    })
    Mono<Double> getAverageResponseTime(List<String> routeIds, Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { " +
        "    routeIdentifier: { $in: ?0 }, " +
        "    timestamp: { $gte: ?1 } " +
        "} }",
        "{ $group: { _id: null, count: { $sum: 1 } } }",
        "{ $project: { _id: 0, requestsPerMinute: { $divide: ['$count', 5] } } }"
    })
    Mono<Double> getRequestsPerMinute(List<String> routeIds, Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { " +
        "    routeIdentifier: { $in: ?0 }, " +
        "    timestamp: { $gte: ?1 } " +
        "} }",
        "{ $group: { " +
        "    _id: null, " +
        "    successCount: { $sum: { $cond: ['$success', 1, 0] } }, " +
        "    total: { $sum: 1 } " +
        "} }",
        "{ $project: { _id: 0, successRate: { $divide: ['$successCount', '$total'] } } }"
    })
    Mono<Double> getSuccessRate(List<String> routeIds, Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: null," +
        "    avgTime: { $avg: '$responseTime' }" +
        "} }"
    })
    Mono<Double> getAverageResponseTime(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 }, 'statusCode': { $gte: 200, $lt: 300 } } }",
        "{ $group: { " +
        "    _id: null," +
        "    count: { $sum: 1 }," +
        "    minTimestamp: { $min: '$timestamp' }," +
        "    maxTimestamp: { $max: '$timestamp' }" +
        "} }",
        "{ $project: { " +
        "    _id: 0," +
        "    requestsPerMinute: { " +
        "        $cond: [ " +
        "            { $eq: [{ $subtract: ['$maxTimestamp', '$minTimestamp'] }, 0] }," +
        "            0," +
        "            { $divide: [" +
        "                { $multiply: ['$count', 60000] }," +
        "                { $subtract: ['$maxTimestamp', '$minTimestamp'] }" +
        "            ] }" +
        "        ]" +
        "    }" +
        "} }"
    })
    Mono<Double> getRequestsPerMinute(Instant cutoff);

    default Mono<Double> getSuccessRate(Instant cutoff) {
        return Mono.zip(
            countSuccessfulRequestsAfter(cutoff),
            countRequestsAfter(cutoff)
        ).map(tuple -> {
            long successful = tuple.getT1();
            long total = tuple.getT2();
            return total == 0 ? 1.0 : (double) successful / total;
        });
    }

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: { $concat: [" +
        "      { $toString: { $subtract: ['$statusCode', { $mod: ['$statusCode', 100] }] } }," +
        "      'xx'" +
        "    ] }," +
        "    count: { $sum: 1 }" +
        "} }",
        "{ $sort: { '_id': 1 } }"
    })
    Flux<ErrorStats> getErrorBreakdown(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: '$routeId'," +
        "    avgResponseTime: { $avg: '$responseTime' }," +
        "    p95ResponseTime: { $percentile: { input: '$responseTime', p: 0.95 } }," +
        "    requestCount: { $sum: 1 }," +
        "    errorCount: { $sum: { $cond: [{ $gte: ['$statusCode', 400] }, 1, 0] } }" +
        "} }",
        "{ $sort: { 'requestCount': -1 } }"
    })
    Flux<RouteStats> getRouteStats(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: { $dateTrunc: { date: '$timestamp', unit: 'minute' } }," +
        "    requestCount: { $sum: 1 }," +
        "    avgResponseTime: { $avg: '$responseTime' }" +
        "} }",
        "{ $sort: { '_id': 1 } }"
    })
    Flux<TimeSeriesStats> getTimeSeriesStats(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: '$toService'," +
        "    avgResponseTime: { $avg: '$responseTime' }," +
        "    requestCount: { $sum: 1 }," +
        "    errorCount: { $sum: { $cond: [{ $gte: ['$statusCode', 400] }, 1, 0] } }," +
        "    p95ResponseTime: { $percentile: { input: '$responseTime', p: 0.95 } }," +
        "    p99ResponseTime: { $percentile: { input: '$responseTime', p: 0.99 } }" +
        "} }",
        "{ $sort: { 'requestCount': -1 } }"
    })
    Flux<ServiceStats> getServiceStats(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: { " +
        "      service: '$toService'," +
        "      hour: { $dateTrunc: { date: '$timestamp', unit: 'hour' } }" +
        "    }," +
        "    requestCount: { $sum: 1 }," +
        "    avgResponseTime: { $avg: '$responseTime' }," +
        "    errorCount: { $sum: { $cond: [{ $gte: ['$statusCode', 400] }, 1, 0] } }" +
        "} }",
        "{ $sort: { '_id.hour': 1 } }"
    })
    Flux<ServiceTimeSeriesStats> getServiceTimeSeriesStats(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: { " +
        "      service: '$toService'," +
        "      path: '$path'," +
        "      method: '$method'" +
        "    }," +
        "    avgResponseTime: { $avg: '$responseTime' }," +
        "    requestCount: { $sum: 1 }," +
        "    errorCount: { $sum: { $cond: [{ $gte: ['$statusCode', 400] }, 1, 0] } }," +
        "    p95ResponseTime: { $percentile: { input: '$responseTime', p: 0.95 } }" +
        "} }",
        "{ $sort: { 'requestCount': -1 } }"
    })
    Flux<EndpointStats> getEndpointStats(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: { " +
        "      service: '$toService'," +
        "      path: '$path'," +
        "      method: '$method'," +
        "      hour: { $dateTrunc: { date: '$timestamp', unit: 'hour' } }" +
        "    }," +
        "    requestCount: { $sum: 1 }," +
        "    avgResponseTime: { $avg: '$responseTime' }," +
        "    errorCount: { $sum: { $cond: [{ $gte: ['$statusCode', 400] }, 1, 0] } }" +
        "} }",
        "{ $sort: { '_id.hour': 1 } }"
    })
    Flux<EndpointTimeSeriesStats> getEndpointTimeSeriesStats(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { " +
        "    routeIdentifier: { $in: ?0 }, " +
        "    timestamp: { $gte: ?1 }, " +
        "    duration: { $exists: true, $ne: null }" +
        "} }",
        "{ $group: { " +
        "    _id: { " +
        "      service: { $ifNull: ['$toService', 'unknown'] }," +
        "      path: { $ifNull: ['$pathEndPoint', '/'] }," +
        "      method: { $ifNull: ['$interactionType', 'APP_TO_APP'] }" +
        "    }," +
        "    responseTimes: { $push: { $ifNull: ['$duration', 0] } }," +
        "    min: { $min: { $ifNull: ['$duration', 0] } }," +
        "    max: { $max: { $ifNull: ['$duration', 0] } }," +
        "    count: { $sum: 1 }" +
        "} }",
        "{ $match: { count: { $gte: 1 } } }",
        "{ $addFields: { " +
        "    sortedTimes: { $sortArray: { input: '$responseTimes', sortBy: 1 } }" +
        "} }",
        "{ $project: { " +
        "    _id: 1," +
        "    min: 1," +
        "    max: 1," +
        "    count: 1," +
        "    p50: { $arrayElemAt: ['$sortedTimes', { $floor: { $multiply: [ 0.50, { $subtract: ['$count', 1] } ] } } ] }," +
        "    p75: { $arrayElemAt: ['$sortedTimes', { $floor: { $multiply: [ 0.75, { $subtract: ['$count', 1] } ] } } ] }," +
        "    p90: { $arrayElemAt: ['$sortedTimes', { $floor: { $multiply: [ 0.90, { $subtract: ['$count', 1] } ] } } ] }," +
        "    p95: { $arrayElemAt: ['$sortedTimes', { $floor: { $multiply: [ 0.95, { $subtract: ['$count', 1] } ] } } ] }," +
        "    p99: { $arrayElemAt: ['$sortedTimes', { $floor: { $multiply: [ 0.99, { $subtract: ['$count', 1] } ] } } ] }" +
        "} }",
        "{ $sort: { 'p95': -1 } }"
    })
    Flux<EndpointLatencyStats> getEndpointLatencyStats(List<String> routeIds, Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: { " +
        "      statusCode: '$statusCode'," +
        "      service: '$toService'" +
        "    }," +
        "    count: { $sum: 1 }," +
        "    avgResponseTime: { $avg: '$responseTime' }," +
        "    errorRate: { " +
        "      $divide: [" +
        "        { $sum: { $cond: [{ $gte: ['$statusCode', 400] }, 1, 0] } }," +
        "        { $sum: 1 }" +
        "      ]" +
        "    }," +
        "    severity: { " +
        "      $switch: { " +
        "        branches: [" +
        "          { case: { $gte: ['$statusCode', 500] }, then: 'CRITICAL' }," +
        "          { case: { $gte: ['$statusCode', 400] }, then: 'WARNING' }" +
        "        ]," +
        "        default: 'INFO'" +
        "      }" +
        "    }" +
        "} }",
        "{ $sort: { 'errorRate': -1, '_id.service': 1 } }"
    })
    Flux<ServiceErrorStats> getServiceErrorStats(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { timestamp: { $gte: ?0 } } }",
        "{ $group: { " +
        "    _id: { " +
        "      service: '$toService'," +
        "      statusCode: '$statusCode'," +
        "      interval: { $dateTrunc: { date: '$timestamp', unit: 'hour' } }" +
        "    }," +
        "    count: { $sum: 1 }," +
        "    avgResponseTime: { $avg: '$responseTime' }," +
        "    errorRate: { " +
        "      $divide: [" +
        "        { $sum: { $cond: [{ $gte: ['$statusCode', 400] }, 1, 0] } }," +
        "        { $sum: 1 }" +
        "      ]" +
        "    }" +
        "} }",
        "{ $sort: { '_id.interval': 1 } }",
        "{ $group: { " +
        "    _id: { " +
        "      service: '$_id.service'," +
        "      statusCode: '$_id.statusCode'" +
        "    }," +
        "    trend: { " +
        "      $push: { " +
        "        interval: '$_id.interval'," +
        "        count: '$count'," +
        "        errorRate: '$errorRate'" +
        "      }" +
        "    }," +
        "    avgErrorRate: { $avg: '$errorRate' }," +
        "    maxErrorRate: { $max: '$errorRate' }," +
        "    minErrorRate: { $min: '$errorRate' }" +
        "} }"
    })
    Flux<ServiceErrorTrend> getServiceErrorTrends(Instant cutoff);

    @Query("{ 'timestamp': { $gte: ?0 } }")
    Flux<ApiMetric> findByTimestampAfter(Instant cutoff);

    @Aggregation(pipeline = {
        "{ $match: { routeIdentifier: { $in: ?0 } } }",
        "{ $group: { _id: '$routeIdentifier', requestCount: { $sum: 1 } } }"
    })
    Flux<ServiceUsageAggregation> getServiceUsageStats(List<String> routeIdentifiers);

    @Query(value = "{ 'routeIdentifier': { $in: ?0 }, 'timestamp': { $gte: ?1 } }", count = true)
    Mono<Long> countMetrics(List<String> routeIds, Instant cutoff);
}

