package org.lite.gateway.entity;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class HealthThresholds {
    @Min(value = 0, message = "CPU threshold must be at least 0")
    @Max(value = 100, message = "CPU threshold cannot exceed 100")
    private double cpuThreshold = 80.0;
    
    @Min(value = 0, message = "Memory threshold must be at least 0")
    @Max(value = 100, message = "Memory threshold cannot exceed 100")
    private double memoryThreshold = 85.0;
    
    @Min(value = 0, message = "Response time threshold must be at least 0")
    private long responseTimeThreshold = 5000; // in milliseconds
    
    private long timeoutThreshold = 5000L;  // 5 seconds timeout
} 