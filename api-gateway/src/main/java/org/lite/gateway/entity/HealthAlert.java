package org.lite.gateway.entity;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class HealthAlert {
    @NotNull(message = "Metric name must be specified")
    private String metric;
    
    @NotNull(message = "Condition must be specified")
    private String condition;  // "above", "below", "equals"
    
    private double threshold;
    
    @NotNull(message = "Severity must be specified")
    private String severity;   // "warning", "critical"
    
    private String message;
} 