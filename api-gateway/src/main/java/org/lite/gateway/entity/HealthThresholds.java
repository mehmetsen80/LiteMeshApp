package org.lite.gateway.entity;

import lombok.Data;

import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class HealthThresholds {
    public HealthThresholds() {
        this.cpuThreshold = 80.0;
        this.memoryThreshold = 80.0;
        this.responseTimeThreshold = 1000;
    }

    @Min(value = 0, message = "CPU threshold must be at least 0")
    @Max(value = 100, message = "CPU threshold cannot exceed 100")
    @Field("cpuThreshold")
    private Double cpuThreshold;
    
    @Min(value = 0, message = "Memory threshold must be at least 0")
    @Max(value = 100, message = "Memory threshold cannot exceed 100")
    @Field("memoryThreshold")
    private Double memoryThreshold;
    
    @Min(value = 0, message = "Response time threshold must be at least 0")
    @Field("responseTimeThreshold")
    private Integer responseTimeThreshold;
    
    @Field("timeoutThreshold")
    private Integer timeoutThreshold = 5000;  // 5 seconds timeout
} 