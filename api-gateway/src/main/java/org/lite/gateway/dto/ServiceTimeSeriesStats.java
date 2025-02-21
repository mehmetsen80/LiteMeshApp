package org.lite.gateway.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ServiceTimeSeriesStats {
    private ServiceTimeId id;
    private Long requestCount;
    private Double avgResponseTime;
    private Long errorCount;

    @Data
    public static class ServiceTimeId {
        private String service;
        private Instant hour;
    }
} 