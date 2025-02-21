package org.lite.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatDTO {
    private String title;
    private String value;
    private String type;
    private StatTrendDTO trend;
}