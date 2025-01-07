package org.lite.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricPoint {
    private String metric;
    private double value;
    private long timestamp;

    public Instant getTimestampAsInstant() {
        return Instant.ofEpochMilli(timestamp);
    }

    // Helper method to create a point with current timestamp
    public static MetricPoint now(String metric, double value) {
        return new MetricPoint(metric, value, System.currentTimeMillis());
    }

    // Helper method to check if point is older than a given duration in milliseconds
    public boolean isOlderThan(long durationMillis) {
        return System.currentTimeMillis() - timestamp > durationMillis;
    }

    // Helper method to get age in milliseconds
    public long getAgeMillis() {
        return System.currentTimeMillis() - timestamp;
    }
} 