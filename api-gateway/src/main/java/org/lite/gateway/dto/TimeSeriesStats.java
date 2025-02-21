package org.lite.gateway.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class TimeSeriesStats {
    private Instant id;  // timestamp
    private Long requestCount;
    private Double avgResponseTime;
} 