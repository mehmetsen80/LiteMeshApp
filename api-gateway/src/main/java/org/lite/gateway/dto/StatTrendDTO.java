package org.lite.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatTrendDTO {
    private double percentChange;
    private String period;
} 