package org.lite.gateway.model;

public record RedisRateLimiterRecord(String routeId, int replenishRate, int burstCapacity, int requestedTokens) {
}
