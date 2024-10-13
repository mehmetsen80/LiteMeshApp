package org.lite.gateway.model;

public record TimeLimiterRecord(String routeId, int timeoutDuration, boolean cancelRunningFuture) { }
