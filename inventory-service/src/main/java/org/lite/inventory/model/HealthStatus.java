package org.lite.inventory.model;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class HealthStatus {
    private String serviceId;          // Add this field
    private String status;          
    private String uptime;          
    private Instant timestamp;      
    private Map<String, Double> metrics;  
} 