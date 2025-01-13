package org.lite.gateway.entity;

import lombok.Data;

import java.util.Map;

// Helper class to represent each filter and its arguments
@Data
public class FilterConfig {
    private String name;                // Filter name, e.g., "CircuitBreaker"
    private Map<String, String> args;
}
