package org.lite.gateway.entity;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "alerts")
@Data
public class Alert {
    @Id
    private String id;
    private String routeId;
    private String metric;
    private String condition;
    private double threshold;
    private LocalDateTime createdAt;
    private boolean active;
    private int consecutiveFailures;
    private Map<String, Double> lastMetrics;
    private String lastErrorMessage;
    private LocalDateTime lastUpdated;
    private String severity;

    public static Alert fromRule(AlertRule rule, String routeId, Map<String, Double> metrics) {
        Alert alert = new Alert();
        alert.setRouteId(routeId);
        alert.setMetric(rule.getMetric());
        alert.setCondition(rule.getCondition());
        alert.setThreshold(rule.getThreshold());
        alert.setCreatedAt(LocalDateTime.now());
        alert.setLastUpdated(LocalDateTime.now());
        alert.setActive(true);
        alert.setLastMetrics(metrics);
        alert.setSeverity(rule.getSeverity().toString());
        
        Double value = metrics.get(rule.getMetric());
        alert.setLastErrorMessage(String.format("%s usage (%.1f%%) exceeded threshold (%.1f%%)", 
            rule.getMetric(), value, rule.getThreshold()));
        
        return alert;
    }
} 