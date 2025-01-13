package org.lite.gateway.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceDTO {
    private String serviceId;
    private String status;
    private ServiceMetrics metrics;
    private ServiceTrends trends;
    private String uptime;
    private String lastChecked;
}

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class ServiceMetrics {
    private double cpu;
    private double memory;
    private long responseTime;
}

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class ServiceTrends {
    private TrendInfo cpu;
    private TrendInfo memory;
    private TrendInfo responseTime;
}

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class TrendInfo {
    private String direction;
    private double percentageChange;
} 