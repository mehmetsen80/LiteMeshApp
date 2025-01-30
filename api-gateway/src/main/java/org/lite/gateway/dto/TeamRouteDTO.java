package org.lite.gateway.dto;

import lombok.Data;
import lombok.Builder;
import org.lite.gateway.entity.RoutePermission;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class TeamRouteDTO {
    private String id;
    private String teamId;
    private String routeId;
    private String routeIdentifier;
    private String path;
    private Integer version;
    private Set<RoutePermission> permissions;
    private LocalDateTime assignedAt;
    private String assignedBy;
} 