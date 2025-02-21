package org.lite.gateway.entity;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class HealthCheckConfig {
    public HealthCheckConfig() {
        this.enabled = true;
        this.interval = 30; // Default 30 seconds
        this.path = "/health";
        this.timeout = 5; // Default 5 seconds
        this.thresholds = new HealthThresholds(); // Default thresholds
        this.alertRules = new ArrayList<>(); // Empty alert rules
        this.requiredMetrics = new ArrayList<>(); // Empty required metrics
    }

    private boolean enabled;
    private int interval;
    private String path;
    private int timeout;
    private HealthThresholds thresholds;
    private List<AlertRule> alertRules;
    private List<String> requiredMetrics;
}