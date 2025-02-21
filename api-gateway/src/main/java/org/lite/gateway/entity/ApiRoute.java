package org.lite.gateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Arrays;


@Document("apiRoutes")
@Data
public class ApiRoute{
    public ApiRoute() {
        // Initialize default values
        this.version = 1;
        this.createdAt = System.currentTimeMillis();
        this.method = "GET";
        this.maxCallsPerDay = 100000;
        
        // Initialize default HealthCheckConfig
        HealthCheckConfig healthCheck = new HealthCheckConfig();
        healthCheck.setEnabled(true);
        healthCheck.setInterval(30);
        healthCheck.setPath("/health");
        healthCheck.setTimeout(5);
        healthCheck.setThresholds(new HealthThresholds());
        healthCheck.setRequiredMetrics(Arrays.asList("cpu", "memory", "responseTime"));
        healthCheck.setAlertRules(createDefaultAlertRules());
        this.healthCheck = healthCheck;
        
        // Initialize empty collections
        this.permissions = new HashSet<>();
        this.filters = createDefaultFilters();
    }

    private List<AlertRule> createDefaultAlertRules() {
        List<AlertRule> alertRules = new ArrayList<>();
        
        // CPU Alert Rule
        alertRules.add(AlertRule.builder()
            .metric("cpu")
            .condition(">=")
            .threshold(80.0)
            .severity(AlertSeverity.WARNING)
            .description("CPU usage is high")
            .build());
        
        // Memory Alert Rule
        alertRules.add(AlertRule.builder()
            .metric("memory")
            .condition(">=")
            .threshold(80.0)
            .severity(AlertSeverity.WARNING)
            .description("Memory usage is high")
            .build());
        
        // Response Time Alert Rule
        alertRules.add(AlertRule.builder()
            .metric("responseTime")
            .condition(">")
            .threshold(1000.0)
            .severity(AlertSeverity.WARNING)
            .description("Response time is above threshold")
            .build());
            
        return alertRules;
    }

    private List<FilterConfig> createDefaultFilters() {
        List<FilterConfig> filters = new ArrayList<>();
        
        // Redis Rate Limiter
        FilterConfig rateLimiter = new FilterConfig();
        rateLimiter.setName("RedisRateLimiter");
        rateLimiter.setArgs(Map.of(
            "replenishRate", "10",
            "burstCapacity", "20",
            "requestedTokens", "1"
        ));
        filters.add(rateLimiter);
        
        // Time Limiter
        FilterConfig timeLimiter = new FilterConfig();
        timeLimiter.setName("TimeLimiter");
        timeLimiter.setArgs(Map.of(
            "timeoutDuration", "30",
            "cancelRunningFuture", "true"
        ));
        filters.add(timeLimiter);
        
        // Circuit Breaker
        FilterConfig circuitBreaker = new FilterConfig();
        circuitBreaker.setName("CircuitBreaker");
        circuitBreaker.setArgs(Map.of(
            "name", this.routeIdentifier + "CircuitBreaker",
            "fallbackUri", "/fallback/" + this.routeIdentifier,
            "slidingWindowSize", "2",
            "failureRateThreshold", "80",
            "waitDurationInOpenState", "PT10S",
            "permittedNumberOfCallsInHalfOpenState", "3",
            "recordFailurePredicate", "HttpResponsePredicate",
            "automaticTransitionFromOpenToHalfOpenEnabled", "true"
        ));
        filters.add(circuitBreaker);
        
        // Retry
        FilterConfig retry = new FilterConfig();
        retry.setName("Retry");
        retry.setArgs(Map.of(
            "maxAttempts", "4",
            "waitDuration", "PT2S",
            "retryExceptions", "java.io.IOException, java.net.SocketTimeoutException, java.lang.RuntimeException"
        ));
        filters.add(retry);
        
        return filters;
    }

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
    
    @JsonIgnore
    private Set<RoutePermission> permissions;
}



