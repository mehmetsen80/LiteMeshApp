package org.lite.gateway.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class EndpointId {
    private String service;
    private String path;
    private String method;
} 