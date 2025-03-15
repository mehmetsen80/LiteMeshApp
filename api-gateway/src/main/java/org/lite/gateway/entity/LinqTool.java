package org.lite.gateway.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "linq_tools")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinqTool {
    @Id
    private String id;
    private String target;            // e.g., "openai"
    private String endpoint;          // e.g., "https://api.openai.com/v1/chat/completions"
    private String method;            // e.g., "POST"
    private Map<String, String> headers; // e.g., {"Authorization": "Bearer YOUR_API_KEY"}
    private List<String> supportedIntents; // e.g., ["generate", "summarize"]
    private String team;              // e.g., "ai_team" (RBAC scope)
}
