package org.lite.gateway.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class EndpointTimeSeriesStats {
    private EndpointTimeId id;
    private Long requestCount;
    private Double avgResponseTime;
    private Long errorCount;

    @Data
    public static class EndpointTimeId {
        private String service;
        private String path;
        private String method;
        private Instant hour;
    }
} 