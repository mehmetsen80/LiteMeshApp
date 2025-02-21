package org.lite.gateway.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ServiceErrorTrend {
    private ServiceErrorId id;
    private List<ErrorInterval> trend;
    private Double avgErrorRate;
    private Double maxErrorRate;
    private Double minErrorRate;

    @Data
    public static class ServiceErrorId {
        private String service;
        private Integer statusCode;
    }

    @Data
    public static class ErrorInterval {
        private Instant interval;
        private Long count;
        private Double errorRate;
    }
} 