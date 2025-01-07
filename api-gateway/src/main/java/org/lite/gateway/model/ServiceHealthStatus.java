package org.lite.gateway.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.*;
import java.time.Instant;
import java.time.Duration;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceHealthStatus {
    @Builder.Default
    private boolean healthy = false;
    @Builder.Default
    private String status = "DOWN";
    @Builder.Default
    private Map<String, Double> metrics = new HashMap<>();
    @Builder.Default
    private long lastChecked = Instant.now().toEpochMilli();
    @Builder.Default
    private int consecutiveFailures = 0;
    private Duration uptime;
    private String serviceId;

    public void updateLastChecked() {
        this.lastChecked = Instant.now().toEpochMilli();
    }

    public void incrementConsecutiveFailures() {
        this.consecutiveFailures++;
    }

    public void resetConsecutiveFailures() {
        this.consecutiveFailures = 0;
    }

    public Set<String> getTrackedMetrics() {
        return metrics != null ? metrics.keySet() : Collections.emptySet();
    }

    public boolean hasMetric(String metric) {
        return metrics.containsKey(metric);
    }

    @JsonProperty("uptime")
    public String getFormattedUptime() {
        if (uptime == null) return "PT0S";
        
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();
        
        StringBuilder formatted = new StringBuilder("P");
        if (days > 0) formatted.append(days).append("D");
        if (hours > 0 || minutes > 0 || seconds > 0) formatted.append("T");
        if (hours > 0) formatted.append(hours).append("H");
        if (minutes > 0) formatted.append(minutes).append("M");
        if (seconds > 0 || (hours == 0 && minutes == 0)) formatted.append(seconds).append("S");
        
        return formatted.toString();
    }
} 