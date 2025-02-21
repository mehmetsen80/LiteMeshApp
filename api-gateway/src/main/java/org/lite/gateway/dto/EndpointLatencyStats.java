package org.lite.gateway.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class EndpointLatencyStats {
    private EndpointId id;
    private Double p50;
    private Double p75;
    private Double p90;
    private Double p95;
    private Double p99;
    private Double min;
    private Double max;
    private long count;
}