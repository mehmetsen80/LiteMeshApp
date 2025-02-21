package org.lite.gateway.dto;

import lombok.Data;
import java.util.Set;

@Data
public class LatencyDistribution {
    private String id;  // latency bucket (e.g., "0-10ms", "10-50ms", etc.)
    private Long count;
    private Set<EndpointInfo> endpoints;

    @Data
    public static class EndpointInfo {
        private String service;
        private String path;
        private String method;
    }
} 