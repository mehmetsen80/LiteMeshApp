package org.lite.gateway.dto;

import lombok.Data;

@Data
public class EndpointErrorStats {
    private EndpointErrorId id;
    private Long count;
    private Double avgResponseTime;

    @Data
    public static class EndpointErrorId {
        private String service;
        private String path;
        private String method;
        private Integer statusCode;
    }
} 