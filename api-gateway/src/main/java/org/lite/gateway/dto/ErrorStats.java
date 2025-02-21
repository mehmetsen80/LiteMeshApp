package org.lite.gateway.dto;

import lombok.Data;

@Data
public class ErrorStats {
    private String id;  // e.g., "4xx" or "5xx"
    private Long count;
} 