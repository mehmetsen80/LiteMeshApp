package org.lite.gateway.dto;

import lombok.Data;

@Data
public class ServiceStats {
    private String id;  // service name
    private Double avgResponseTime;
    private Double p95ResponseTime;
    private Double p99ResponseTime;
    private Long requestCount;
    private Long errorCount;
} 