package org.lite.gateway.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {
    @NotNull(message = "Metric name must be specified")
    @Field("metric")
    private String metric;

    @NotNull(message = "Condition must be specified")
    @Pattern(regexp = ">=|<=|>|<|==", message = "Invalid condition. Must be one of: >=, <=, >, <, ==")
    @Field("condition")
    private String condition;

    @NotNull(message = "Threshold must be specified")
    @Field("threshold")
    private Double threshold;

    @Field("description")
    private String description;
    
    private AlertSeverity severity = AlertSeverity.WARNING;
}