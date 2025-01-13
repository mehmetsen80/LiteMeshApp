package org.lite.gateway.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class RouteChangeDetails {
    private String summary;
    private String description;
    private Map<String, Object> changedFields;
    private String changeContext;
} 