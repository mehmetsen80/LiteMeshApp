package org.lite.gateway.entity;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document("routeVersionMetadata")
public class RouteVersionMetadata {
    @Id
    private String id;
    private String routeIdentifier;
    private Integer version;
    private String changeReason;
    private String changeDescription;
    private Map<String, Object> changedFields;
    private String changedBy;
    private Long timestamp;
    private ChangeType changeType;

    public enum ChangeType {
        CREATE,
        UPDATE,
        ROLLBACK
    }
} 