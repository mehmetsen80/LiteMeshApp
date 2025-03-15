package org.lite.gateway.entity;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.lite.gateway.enums.UserRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "team_members")
@CompoundIndexes({
    @CompoundIndex(name = "team_user_idx", def = "{'teamId': 1, 'userId': 1}", unique = true)
})
public class TeamMember {
    @Id
    private String id;
    
    private String teamId;
    private String userId;
    @Transient
    private String username;
    private UserRole role;
    private TeamMemberStatus status;
    
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    @Builder.Default
    private LocalDateTime lastActiveAt = LocalDateTime.now();
}
