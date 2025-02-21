package org.lite.gateway.dto;

import lombok.Data;

@Data
public class ServiceErrorStats {
    private ServiceErrorId id;
    private Long count;
    private Double avgResponseTime;
    private Double errorRate;
    private String severity;

    @Data
    public static class ServiceErrorId {
        private Integer statusCode;
        private String service;
    }
} 