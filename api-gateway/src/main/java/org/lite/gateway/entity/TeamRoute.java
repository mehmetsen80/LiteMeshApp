package org.lite.gateway.entity;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "team_routes")
@CompoundIndexes({
    @CompoundIndex(name = "team_route_idx", def = "{'teamId': 1, 'routeId': 1}", unique = true)
})
public class TeamRoute {
    @Id
    private String id;
    
    private String teamId;
    private String routeId;
    
    @Builder.Default
    private Set<RoutePermission> permissions = Set.of(RoutePermission.VIEW);
    
    private LocalDateTime assignedAt;
    private String assignedBy;
} 