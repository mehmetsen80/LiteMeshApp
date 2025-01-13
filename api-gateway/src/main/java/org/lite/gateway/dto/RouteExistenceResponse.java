package org.lite.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteExistenceResponse {
    private boolean exists;
    private String message;
    private ExistenceDetail detail;

    @Data
    @Builder
    public static class RouteDetail {
        private String id;
        private String routeIdentifier;
        private String uri;
        private String method;
        private String path;
        private Long createdAt;
        private Long updatedAt;
        private Integer version;
    }

    @Data
    @Builder
    public static class ExistenceDetail {
        private boolean idExists;
        private boolean identifierExists;
        private String existingId;
        private String existingIdentifier;
        private RouteDetail existingRoute;
        private String validationMessage;
    }
} 