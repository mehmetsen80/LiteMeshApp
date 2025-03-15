package org.lite.gateway.dto;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamInfoDTO {
    private String teamId;
    private String teamName;
    private String organizationId;
    private String organizationName;
} 