package org.lite.gateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;


@Document("apiRoutes")
@Data
public class ApiRoute{
    @Id String id;
    @Min(value = 1, message = "Version must be at least 1")
    private Integer version = 1;
    @NotNull(message = "Creation timestamp is required")
    private Long createdAt = System.currentTimeMillis();
    private Long updatedAt;
    @NotBlank(message = "Route identifier is required")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Route identifier can only contain letters, numbers, and hyphens")
    String routeIdentifier;
    @NotBlank(message = "URI is required")
    String uri;
    String method;
    @NotBlank(message = "Path is required")
    String path;
    @NotBlank(message = "Scope is required")
    String scope;
    @Min(value = 1, message = "Max calls per day must be at least 1")
    Integer maxCallsPerDay;
    @Valid
    List<FilterConfig> filters;
    @Valid
    @NotNull(message = "Health check configuration is required")
    HealthCheckConfig healthCheck;
    private Set<RoutePermission> permissions;
}



