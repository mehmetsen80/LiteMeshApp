package org.lite.gateway.dto;

import lombok.Data;
import lombok.Builder;
import org.lite.gateway.entity.TeamMemberStatus;
import org.lite.gateway.entity.TeamRole;
import java.time.LocalDateTime;

@Data
@Builder
public class TeamMemberDTO {
    private String id;
    private String teamId;
    private String userId;
    private String username;
    private TeamRole role;
    private TeamMemberStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
} 