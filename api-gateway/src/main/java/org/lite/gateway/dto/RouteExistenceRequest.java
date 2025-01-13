package org.lite.gateway.dto;

import lombok.Data;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class RouteExistenceRequest {
    @Size(min = 1, max = 64, message = "ID must be between 1 and 64 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "ID can only contain letters, numbers, hyphens, and underscores")
    private String id;

    @Size(min = 1, max = 128, message = "Route identifier must be between 1 and 128 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Route identifier can only contain letters, numbers, and hyphens")
    private String routeIdentifier;
} 