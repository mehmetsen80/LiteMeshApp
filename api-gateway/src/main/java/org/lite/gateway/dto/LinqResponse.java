package org.lite.gateway.dto;

import lombok.Data;

@Data
public class LinqResponse {
    private Object result; // e.g., "Hello from inventory!"
    private Metadata metadata;

    @Data
    public static class Metadata {
        private String source; // e.g., "inventory-service"
        private String status; // e.g., "success"
        private String team;   // e.g., "inventory_team" (from gateway context)
        private boolean cacheHit; // e.g., false
    }
}

