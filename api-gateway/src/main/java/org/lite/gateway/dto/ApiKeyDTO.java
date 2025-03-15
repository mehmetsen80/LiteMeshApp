package org.lite.gateway.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ApiKeyDTO {
    private String name;
    private String key;
    private String createdBy;
    private LocalDateTime createdAt;
} 