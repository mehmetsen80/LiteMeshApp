package org.lite.gateway.entity;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
public class AlertRule {
    @NotNull(message = "Metric name must be specified")
    private String metric;

    @NotNull(message = "Condition must be specified")
    @Pattern(regexp = "^[<>]=?|=$", message = "Condition must be one of: >, >=, <, <=, =")
    private String condition;

    @NotNull(message = "Threshold must be specified")
    private double threshold;

    private String description;
    
    private AlertSeverity severity = AlertSeverity.WARNING;
}