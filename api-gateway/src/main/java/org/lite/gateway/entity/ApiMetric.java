package org.lite.gateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document("apiMetrics")
public class ApiMetric {
    @Id
    private String id;
    private String fromService; //originated microservice
    private String toService; //target microservice
    private LocalDateTime timestamp;
    private long duration; // in milliseconds
    private String routeIdentifier;  // unique route identifier from ApiRoute
    private String interactionType;  // APP_TO_APP or USER_TO_APP
    private String gatewayBaseUrl;   // The base URL of the gateway
    private String pathEndPoint;     // Endpoint path after the base URL
    private String queryParameters;  // Parameters for GET requests
    private String requestPayload;   // Body data for POST requests
    private boolean success;         // false if exception is thrown
}
