package org.lite.gateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "api_keys")
public class ApiKey {
    @Id
    private String id;
    private String key;
    private String name;
    private String teamId;
    private String createdBy;
    private Instant createdAt;
    private Instant expiresAt;    // Will be null for infinite/never expires
    private boolean enabled = true;  // Default value set to true
} 