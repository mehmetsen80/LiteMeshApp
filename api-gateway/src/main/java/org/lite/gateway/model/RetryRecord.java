package org.lite.gateway.model;

import java.time.Duration;


public record RetryRecord(String routeId, int maxAttempts, Duration waitDuration, String retryExceptions) { }
