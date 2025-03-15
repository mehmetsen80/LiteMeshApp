package org.lite.gateway.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ApiKeyResponse {
    private String id;
    private String key;
    private String name;
    private String teamId;
    private String createdBy;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean enabled;
} 