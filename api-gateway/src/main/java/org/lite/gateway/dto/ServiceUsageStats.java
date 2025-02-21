package org.lite.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceUsageStats {
    private String service;
    private Long requestCount;
} 