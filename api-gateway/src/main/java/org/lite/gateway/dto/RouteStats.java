package org.lite.gateway.dto;

import lombok.Data;

@Data
public class RouteStats {
    private String id;  // routeId
    private Double avgResponseTime;
    private Double p95ResponseTime;
    private Long requestCount;
    private Long errorCount;
} 