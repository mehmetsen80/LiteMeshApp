package org.lite.gateway.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.lite.gateway.entity.TeamStatus;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDTO {
    private String id;
    private String name;
    private String description;
    private TeamStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private List<TeamMemberDTO> members;
    private List<TeamRouteDTO> routes;
    private OrganizationDTO organization;
    private List<String> roles;
    private ApiKeyDTO apiKey;
} 