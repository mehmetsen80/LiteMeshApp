package org.lite.gateway.dto;

import lombok.Data;

@Data
public class EndpointStats {
    private EndpointId id;
    private Double avgResponseTime;
    private Double p95ResponseTime;
    private Long requestCount;
    private Long errorCount;

    @Data
    public static class EndpointId {
        private String service;
        private String path;
        private String method;
    }
} 