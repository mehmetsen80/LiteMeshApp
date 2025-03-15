package org.lite.gateway.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CreateApiKeyRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Team ID is required")
    private String teamId;
    
    @NotBlank(message = "Creator username is required")
    private String createdBy;
    
    // Optional: if you want to support custom expiration
    private Long expiresInDays;
} 