package org.lite.gateway.model;

import java.time.Duration;

public record CircuitBreakerRecord(String routeId, String cbName, int slidingWindowSize, float failureRateThreshold, Duration waitDurationInOpenState, int permittedCallsInHalfOpenState, String fallbackUri, String recordFailurePredicate, boolean automaticTransition) {
}
