package org.lite.gateway.dto;

import lombok.Data;
import org.lite.gateway.dto.EndpointTimeSeriesStats.EndpointTimeId;

@Data
public class EndpointTrafficStats {
    private EndpointTimeId id;
    private Long requestCount;
    private Double peakRps;  // peak requests per second
} 