package org.lite.gateway.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.entity.RoutePermission;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRouteDTO {
    private String id;
    private TeamInfoDTO team;
    private String routeId;
    private String routeIdentifier;
    private String path;
    private Integer version;
    private Set<RoutePermission> permissions;
    private LocalDateTime assignedAt;
    private String assignedBy;
    private String method;
    private List<FilterConfig> filters;
    private String uri;
    private Integer maxCallsPerDay;
    private boolean healthCheckEnabled;
} 