package org.lite.gateway.entity;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class HealthCheckConfig {
    private boolean enabled = true;
    
    @NotNull(message = "Health check endpoint must be specified")
    private String endpoint = "/health";
    
    private List<String> requiredMetrics = new ArrayList<>();  // e.g., "cpu", "memory", "responseTime"
    
    private HealthThresholds thresholds = new HealthThresholds();
    
    private List<AlertRule> alertRules = new ArrayList<>();
}