package org.lite.gateway.entity;

import lombok.Data;

import java.util.Map;

// Helper class to represent each filter and its arguments
@Data
public class FilterConfig {
    String name;                // Filter name, e.g., "CircuitBreaker"
    Map<String, String> args;
}
